package com.jbrisbin.groovy.mqdsl;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.GroovyScriptEngine;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA. User: jbrisbin Date: Mar 31, 2010 Time: 10:27:03 AM To change this template use File |
 * Settings | File Templates.
 */
@SuppressWarnings({"unchecked"})
public class RabbitMQDsl {

  static Logger log = LoggerFactory.getLogger( RabbitMQDsl.class );
  static Options cliOpts = new Options();
  static Connection mqConnection;
  static Channel mqChannel;

  static {
    cliOpts.addOption( "f", true, "RabbitMQ DSL file to evaluate." );
    cliOpts.addOption( "o", true, "Pipe return message to this file." );
    cliOpts.addOption( "h", true, "Host name of the RabbitMQ server to connect to." );
    cliOpts.addOption( "p", true, "Port of the RabbitMQ server to connect to." );
    cliOpts.addOption( "v", true, "Virtual host of the RabbitMQ server to connect to." );
    cliOpts.addOption( "U", true, "Username for RabbitMQ connections." );
    cliOpts.addOption( "P", true, "Password for the RabbitMQ connections." );
  }

  public static void main( String[] argv ) {

    // Parse command line arguments
    CommandLine args = null;
    try {
      Parser p = new BasicParser();
      args = p.parse( cliOpts, argv );
    } catch ( ParseException e ) {
      log.error( e.getMessage(), e );
    }

    // Runtime properties
    Properties props = System.getProperties();

    // Check for ~/.rabbitmqrc
    File userSettings = new File( System.getProperty( "user.home" ), ".rabbitmqrc" );
    if ( userSettings.exists() ) {
      try {
        props.load( new FileInputStream( userSettings ) );
      } catch ( IOException e ) {
        log.error( e.getMessage(), e );
      }
    }

    // Load Groovy builder file
    StringBuffer script = new StringBuffer();
    BufferedInputStream in = null;
    String filename = "<STDIN>";
    if ( args.hasOption( "f" ) ) {
      filename = args.getOptionValue( "f" );
      try {
        in = new BufferedInputStream( new FileInputStream( filename ) );
      } catch ( FileNotFoundException e ) {
        log.error( e.getMessage(), e );
      }
    } else {
      in = new BufferedInputStream( System.in );
    }

    // Read script
    if ( null != in ) {
      byte[] buff = new byte[4096];
      try {
        for ( int read = in.read( buff ); read > -1; ) {
          script.append( new String( buff, 0, read ) );
          read = in.read( buff );
        }
      } catch ( IOException e ) {
        log.error( e.getMessage(), e );
      }
    } else {
      System.err.println( "No script file to evaluate..." );
    }

    BufferedOutputStream out = null;
    if ( args.hasOption( "o" ) ) {
      try {
        out = new BufferedOutputStream( new FileOutputStream( args.getOptionValue( "o" ) ) );
      } catch ( FileNotFoundException e ) {
        log.error( e.getMessage(), e );
      }
    } else {
      out = new BufferedOutputStream( System.out );
    }

    String[] includes = (System.getenv().containsKey( "MQDSL_INCLUDE" ) ? System.getenv( "MQDSL_INCLUDE" )
        .split( String.valueOf( File.pathSeparatorChar ) ) : new String[]{"."});
    try {
      GroovyScriptEngine groovyEngine = new GroovyScriptEngine( includes );
      Binding binding = new Binding( args.getArgs() );
      RabbitMQBuilder builder = new RabbitMQBuilder();

      // Setup RabbitMQ
      String username = (args.hasOption( "U" ) ? args.getOptionValue( "U" ) : props
          .getProperty( "mq.user", "rabbitmq" ));
      String password = (args.hasOption( "P" ) ? args.getOptionValue( "P" ) : props.getProperty( "mq.password", "" ));
      String virtualHost = (args.hasOption( "v" ) ? args.getOptionValue( "v" ) : props
          .getProperty( "mq.virtualhost", null ));
      String host = (args.hasOption( "h" ) ? args.getOptionValue( "h" ) : props.getProperty( "mq.host", null ));
      int port = Integer.parseInt( args.hasOption( "p" ) ? args.getOptionValue( "p" ) : props.getProperty( "mq.port",
          "5672" ) );

      ConnectionParameters mqConnParams = new ConnectionParameters();
      mqConnParams.setUsername( username );
      mqConnParams.setPassword( password );
      if ( null != virtualHost ) {
        mqConnParams.setVirtualHost( virtualHost );
      }
      mqConnection = new ConnectionFactory( mqConnParams ).newConnection( host, port );
      mqChannel = mqConnection.createChannel();

      builder.setChannel( mqChannel );
      binding.setVariable( "mq", builder );
      binding.setVariable( "stdout", out );
      binding.setVariable( "log", LoggerFactory.getLogger( filename ) );

      GroovyShell shell = new GroovyShell( binding );
      try {
        shell.evaluate( new StringReader( script.toString() ) );
      } catch ( Throwable t ) {
        builder.dispatchError( t );
      }

      boolean active = true;
      while ( active ) {
        try {
          mqChannel.wait( 500 );
        } catch ( InterruptedException e ) {
          log.error( e.getMessage(), e );
        }
        for ( Future f : builder.getConsumers() ) {
          if ( !active && !f.isDone() ) {
            active = true;
          } else {
            active = !f.isDone();
          }
        }
      }

      mqChannel.close();
      mqConnection.close();

      out.flush();
      out.close();

    } catch ( IOException e ) {
      e.printStackTrace();
      System.exit( 1 );
    }

    System.exit( 0 );
  }

}
