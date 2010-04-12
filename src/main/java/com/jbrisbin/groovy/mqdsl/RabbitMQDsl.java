/*
 * Copyright (c) 2010 by J. Brisbin <jon@jbrisbin.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    cliOpts.addOption( "?", "help", false, "Usage instructions." );
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

    // Check for help
    if ( args.hasOption( '?' ) ) {
      printUsage();
      return;
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
      GroovyScriptEngine engine = new GroovyScriptEngine( includes );
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
      binding.setVariable( "publish", new PublishClosure( binding, mqConnection ) );
      binding.setVariable( "stdout", out );
      binding.setVariable( "log", LoggerFactory.getLogger( filename.replaceAll( "\\.g$", "" ) ) );

      GroovyShell shell = new GroovyShell( binding );
      try {
        shell.evaluate( new StringReader( script.toString() ) );
      } catch ( Throwable t ) {
        builder.dispatchError( t );
      }

      while ( builder.isActive() ) {
        try {
          synchronized (mqChannel) {
            mqChannel.wait( 500 );
          }
        } catch ( InterruptedException e ) {
          log.error( e.getMessage(), e );
        }
      }

      try {
        mqChannel.close();
        mqConnection.close();

        out.flush();
        out.close();
      } catch ( Throwable ignored ) {
      }

    } catch ( IOException e ) {
      e.printStackTrace();
      System.exit( 1 );
    } finally {
      System.exit( 0 );
    }
  }

  public static void printUsage() {
    System.err
        .println(
            "Usage: mqdsl [-h MQHOST [-p MQPORT] -U MQUSER -P MQPASS -v MQVHOST] [-f <file to execute>] [-o <output file>]" );
    System.err.println( "   or: cat <file to execute> | mqdsl -o <output file>" );
  }

}
