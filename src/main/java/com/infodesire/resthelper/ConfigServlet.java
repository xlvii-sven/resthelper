// (C) 1998-2015 Information Desire Software GmbH
// www.infodesire.com

package com.infodesire.resthelper;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.net.UrlEscapers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;


/**
 * Manage configuration of the wabapp
 *
 */
public class ConfigServlet extends HttpServlet {


  private static final long serialVersionUID = -7949041376159361010L;
  
  
  private static Logger logger = Logger.getLogger( ConfigServlet.class );
  
  
  // TODO: this might into the web.xml or setup code
  class Config {
    static final String title = "Configuration";
    static final String installKeyTitle = "Install a new Application Key";
    static final String installKeyText = "Copy the application key you obtained from the REST server into this field and confirm.";
    static final String reloadStorage = "Reload storage";
    static final String reloadStorageText = "To reload the stored configuration from filesystem, press the button.";
  }


  private AppProperties appProperties;


  private String applicationId;


  private String configBaseDir; 
  
  
  protected void doPost( HttpServletRequest request,
    HttpServletResponse response ) throws ServletException, IOException {

    response.setContentType( "text/html;charset=utf-8" );
    response.setStatus( HttpServletResponse.SC_OK );
    
    String flashMessage = null;
    String cmd = request.getParameter( "cmd" );
    if( cmd != null ) {
      if( cmd.equals( "install" ) ) {
        String key = request.getParameter( "applicationKey" );
        if( !Strings.isNullOrEmpty( key ) ) {
          getAppProperties().setApplicationKey( key );
          flashMessage = "Key was stored and will be used from now on.";
        }
        else {
          flashMessage = "Empty key not allowed.";
        }
      }
      else if( cmd.equals( "save" ) ) {
        String restUrl = request.getParameter( "restUrl" );
        if( !Strings.isNullOrEmpty( restUrl ) ) {
          getAppProperties().setRestURL( restUrl );
          flashMessage = "The new URL was stored.";
        }
        else {
          flashMessage = "Empty url not allowed.";
        }
      }
      else if( cmd.equals( "reload" ) ) {
        getAppProperties().reload();
        flashMessage = "Key was reloaded.";
      }
    }

    boolean dev = false;
    if( dev ) {
      debug( request, response );
    }
    else {
      if( flashMessage != null ) {
        response.sendRedirect( request.getRequestURI() + "?flash="
          + UrlEscapers.urlFormParameterEscaper().escape( flashMessage ) );
      }
      else {
        response.sendRedirect( request.getRequestURI() );
      }
    }
    
  }
  
  
  private void debug( HttpServletRequest request, HttpServletResponse response ) throws IOException {

    PrintWriter writer = response.getWriter();
    
    head( writer );
    try {
      new PreparedRequest( request ).toHTML( writer );
    }
    catch( URISyntaxException ex ) {
      errorPage( ex, request, response );
    }
    
    foot( writer );
    
  }


  protected void doGet( HttpServletRequest request,
    HttpServletResponse response ) throws ServletException, IOException {

    try {
      
      response.setContentType( "text/html;charset=utf-8" );
      response.setStatus( HttpServletResponse.SC_OK );
      
      PrintWriter writer = response.getWriter();
      
      head( writer );
      
      String flashMessage = new PreparedRequest( request ).getQueryParam( "flash" );
      
      if( flashMessage != null ) {
        writer.println( "<div style=\"border: 1px solid blue;\"><a href=\""
          + request.getRequestURI() + "\">[X]</a> &nbsp; &nbsp;"
          + flashMessage + "</div>" );
      }
      
      writer.println( "<h1>" + Config.title + "</h1>" );
      writer.println( "<table>" );
      
      writer.println( "<form action=\"#\" method=\"POST\">" );
      writer.println( "<table>" );
      writer.println( "<tr>" );
      writer.println( "<td><b>Application id</b></td>" );
      writer.println( "<td>" + applicationId + "</td>" );
      writer.println( "</tr><tr>" );
      writer.println( "<td><b>REST server URL</b></td>" );
      String restUrl = getAppProperties().getRestURL();
      writer
        .println( "<td><input type=\"text\" name=\"restUrl\" cols=\"200\" value=\""
          + ( restUrl == null ? "" : restUrl ) + "\"></input></td>" );
      writer.println( "</tr><tr>" );
      writer.println( "<input type=\"hidden\" name=\"cmd\" value=\"save\"></input>" );
      writer.println( "<td><input type=\"submit\" value=\"Save\"></input></td>" );
      writer.println( "</tr>" );
      writer.println( "</table>" );
      writer.println( "</form>" );
      
      writer.println( "<hr>" );
      writer.println( "<h2>" + Config.installKeyTitle + "</h2>" );
      writer.println( "<div>" + Config.installKeyText + "</div>" );

      writer.println( "<br>" );
      writer.println( "<div><b>Application Key:</b>" );
      writer.println( "<br>" );
      writer.println( "<form action=\"#\" method=\"POST\">" );
      writer.println( "<input type=\"hidden\" name=\"cmd\" value=\"install\"></input>" );
      writer.println( "<input type=\"password\" name=\"applicationKey\" cols=\"100\" rows=\"3\" ></input>" );
      writer.println( "<br>" );
      writer.println( "<input type=\"submit\" value=\"Install\"></input>" );
      writer.println( "</form>" );
      
      writer.println( "<hr>" );
      
      writer.println( "<h2>" + Config.reloadStorage + "</h2>" );
      
      writer.println( "<div>" );
      writer.println( "<form action=\"#\" method=\"POST\">" );
      writer.println( "<input type=\"hidden\" name=\"cmd\" value=\"reload\"></input>" );
      writer.println( "<input type=\"submit\" value=\"Reload\"></input>" );
      writer.println( "</form>" );
      writer.println( "</div>" );

      foot( writer );
      writer.close();

    }
    catch( IOException ex ) {
      errorPage( ex, request, response );
    }
    catch( Exception ex ) {
      errorPage( ex, request, response );
    }
    
  }


  private void head( PrintWriter writer ) {
    writer.println( "<html><head>" );
    writer.println( "<title>" + Config.title + "</title>" );
    writer.println( "<link rel=\"icon\" type=\"image/ico\" href=\"/favicon.ico\"/>" );
    writer.println( "</head><body>" );
  }


  private void foot( PrintWriter writer ) {
    writer.println( "</body></html>" );
  }

  
  private void errorPage( Exception ex, HttpServletRequest request,
    HttpServletResponse response ) {

    try {

      response.setContentType( "text/html;charset=utf-8" );
      response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );

      PrintWriter writer = response.getWriter();
      
      head( writer );
      
      writer.println( "<h1>Internal Server Error</h1>" );
      writer.println( "<div>" );
      writer.println( "<pre>" );
      writer.println( Throwables.getStackTraceAsString( ex ) );
      writer.println( "</pre>" );
      writer.println( "</div>" );

      foot( writer );
      
      writer.close();

    }
    catch( Exception ex1 ) {
      logger.fatal( "Error writing the error page", ex1 );
    }

  }


  @Override
  public void init( ServletConfig config ) throws ServletException {
    
    applicationId = config.getInitParameter( "applicationId" );
    configBaseDir = config.getInitParameter( "configBaseDir" );

  }
  
  
  /**
   * Lazy evaluation is important, because some apps might want to set a system property
   */
  private AppProperties getAppProperties() {
    if( appProperties == null ) {
      File baseDir = BaseDir.getBaseDir( configBaseDir );
      appProperties = new AppProperties( baseDir, applicationId );
    }
    return appProperties;
  }


}


