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

import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Unpacks NAR files. Unpacking happens in the local repository, and also sets flags on binaries and corrects static
 * libraries.
 * 
 * @goal nar-testUnpack
 * @phase process-test-sources
 * @requiresProject
 * @requiresDependencyResolution test
 * @author Mark Donszelmann
 */
public class NarTestUnpackMojo
    extends AbstractUnpackMojo
{

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        NarManager mgr = getNarManager();
        List narArtifacts = mgr.getNarDependencies( "test" );
        if ( classifiers == null )
        {
            mgr.unpackAttachedNars( narArtifacts, archiverManager, null, getOS(), getLayout(), getTestUnpackDirectory() );
        }
        else
        {
            for ( Iterator j = classifiers.iterator(); j.hasNext(); )
            {
                mgr.unpackAttachedNars( narArtifacts, archiverManager, (String) j.next(), getOS(), getLayout(), getTestUnpackDirectory() );
            }
        }
    }
}
