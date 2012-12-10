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

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;

/**
 * Logger to connect the Ant logging to the Maven logging.
 * 
 * @author Mark Donszelmann
 */
public class NarLogger
    implements BuildListener
{

    private Log log;

    public NarLogger( Log log )
    {
        this.log = log;
    }

    public void buildStarted( BuildEvent event )
    {
    }

    public void buildFinished( BuildEvent event )
    {
    }

    public void targetStarted( BuildEvent event )
    {
    }

    public void targetFinished( BuildEvent event )
    {
    }

    public void taskStarted( BuildEvent event )
    {
    }

    public void taskFinished( BuildEvent event )
    {
    }

    public final void messageLogged( BuildEvent event )
    {
        String msg = event.getMessage();
        switch ( event.getPriority() )
        {
            case Project.MSG_ERR:
                if ( msg.indexOf( "ar: creating archive" ) >= 0 )
                {
                    log.debug( msg );
                }
                else if ( msg.indexOf( "warning" ) >= 0 )
                {
                    log.warn( msg );
                }
                else
                {
                    log.error( msg );
                }
                break;
            case Project.MSG_WARN:
                log.warn( msg );
                break;
            case Project.MSG_INFO:
                if ( ( msg.indexOf( "files were compiled" ) >= 0 ) || ( msg.indexOf( "Linking..." ) >= 0 ) )
                {
                    log.info( msg );
                }
                else if ( msg.indexOf( "error" ) >= 0 )
                {
                    log.error( msg );
                }
                else if ( msg.indexOf( "warning" ) >= 0 )
                {
                    log.warn( msg );
                }
                else
                {
                    log.debug( msg );
                }
                break;
            case Project.MSG_VERBOSE:
                log.debug( msg );
                break;
            default:
            case Project.MSG_DEBUG:
                log.debug( msg );
                break;
        }
    }
}
