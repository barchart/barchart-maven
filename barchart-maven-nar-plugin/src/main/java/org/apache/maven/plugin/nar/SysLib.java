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

import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;
import net.sf.antcontrib.cpptasks.types.SystemLibrarySet;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;

/**
 * Keeps info on a system library
 * 
 * @author Mark Donszelmann
 */
public class SysLib
{
    /**
     * Name of the system library
     * 
     * @parameter expression=""
     * @required
     */
    private String name;

    /**
     * Type of linking for this system library
     * 
     * @parameter expression="" default-value="shared"
     * @required
     */
    private String type = Library.SHARED;

    public final SystemLibrarySet getSysLibSet( Project antProject )
        throws MojoFailureException
    {
        if ( name == null )
        {
            throw new MojoFailureException( "NAR: Please specify <Name> as part of <SysLib>" );
        }
        SystemLibrarySet sysLibSet = new SystemLibrarySet();
        sysLibSet.setProject( antProject );
        sysLibSet.setLibs( new CUtil.StringArrayBuilder( name ) );
        LibraryTypeEnum sysLibType = new LibraryTypeEnum();
        sysLibType.setValue( type );
        sysLibSet.setType( sysLibType );
        return sysLibSet;
    }
}
