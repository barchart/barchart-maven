package org.apache.maven.plugin.nar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * @author Mark Donszelmann
 */
public class NarInfo
{

    public static final String NAR_PROPERTIES = "nar.properties";

    private String groupId, artifactId, version;

    private Properties info;

    private Log log;

    public NarInfo( String groupId, String artifactId, String version, Log log ) throws MojoExecutionException
    {
        this( groupId, artifactId, version, log, null );
    }
    
    public NarInfo( String groupId, String artifactId, String version, Log log, File propertiesFile ) throws MojoExecutionException
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.log = log;
        info = new Properties();

        // Fill with general properties.nar file
        if( propertiesFile != null )
        {
            try
            {
                info.load( new FileInputStream( propertiesFile ) );
            }
            catch ( FileNotFoundException e )
            {
                // ignored
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Problem loading "+propertiesFile, e );
            }
        }
    }

    public final String toString()
    {
        StringBuffer s = new StringBuffer( "NarInfo for " );
        s.append( groupId );
        s.append( ":" );
        s.append( artifactId );
        s.append( "-" );
        s.append( version );
        s.append( " {\n" );

        for ( Iterator i = info.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();
            s.append( "   " );
            s.append( key );
            s.append( "='" );
            s.append( info.getProperty( key, "<null>" ) );
            s.append( "'\n" );
        }

        s.append( "}\n" );
        return s.toString();
    }

    public final boolean exists( JarFile jar )
    {
        return getNarPropertiesEntry( jar ) != null;
    }

    public final void read( JarFile jar )
        throws IOException
    {
        info.load( jar.getInputStream( getNarPropertiesEntry( jar ) ) );
    }

    private JarEntry getNarPropertiesEntry( JarFile jar )
    {
        return jar.getJarEntry( "META-INF/nar/" + groupId + "/" + artifactId + "/" + NAR_PROPERTIES );
    }

    /**
     * No binding means default binding.
     * 
     * @param aol
     * @return
     */
    public final String getBinding( AOL aol, String defaultBinding )
    {
        return getProperty( aol, "libs.binding", defaultBinding );
    }

    public final void setBinding( AOL aol, String value )
    {
        setProperty( aol, "libs.binding", value );
    }

    // FIXME replace with list of AttachedNarArtifacts
    public final String[] getAttachedNars( AOL aol, String type )
    {
        String attachedNars = getProperty( aol, NarConstants.NAR+"." + type );
        return attachedNars != null ? attachedNars.split( "," ) : null;
    }

    public final void addNar( AOL aol, String type, String nar )
    {
        String nars = getProperty( aol, NarConstants.NAR+"." + type );
        nars = ( nars == null ) ? nar : nars + ", " + nar;
        setProperty( aol, NarConstants.NAR+"." + type, nars );
    }

    public final void setNar( AOL aol, String type, String nar )
    {
        setProperty( aol, NarConstants.NAR+"." + type, nar );
    }

    public final AOL getAOL( AOL aol )
    {
        return aol == null ? null : new AOL( getProperty( aol, aol.toString(), aol.toString() ) );
    }

    public final String getOptions( AOL aol )
    {
        return getProperty( aol, "linker.options" );
    }

    public final String getLibs( AOL aol )
    {
        return getProperty( aol, "libs.names", artifactId + "-" + version );
    }

    public final String getSysLibs( AOL aol )
    {
        return getProperty( aol, "syslibs.names" );
    }

    public final void writeToFile( File file )
        throws IOException
    {
        info.store( new FileOutputStream( ( file ) ), "NAR Properties for " + groupId + "." + artifactId + "-"
            + version );
    }

    private void setProperty( AOL aol, String key, String value )
    {
        if ( aol == null )
        {
            info.setProperty( key, value );
        }
        else
        {
            info.setProperty( aol.toString() + "." + key, value );
        }
    }

    public final String getProperty( AOL aol, String key )
    {
        return getProperty( aol, key, (String) null );
    }

    public final String getProperty( AOL aol, String key, String defaultValue )
    {
        if ( key == null )
        {
            return defaultValue;
        }
        String value = info.getProperty( key, defaultValue );
        value = aol == null ? value : info.getProperty( aol.toString() + "." + key, value );
        log.debug( "getProperty(" + aol + ", " + key + ", " + defaultValue + ") = " + value );
        return value;
    }

    public final int getProperty( AOL aol, String key, int defaultValue )
    {
        return Integer.parseInt( getProperty( aol, key, Integer.toString( defaultValue ) ) );
    }

    public final boolean getProperty( AOL aol, String key, boolean defaultValue )
    {
        return (new Boolean( getProperty( aol, key, String.valueOf( defaultValue ) ) )).booleanValue();
    }

    public final File getProperty( AOL aol, String key, File defaultValue )
    {
        return new File( getProperty( aol, key, defaultValue.getPath() ) );
    }
}
