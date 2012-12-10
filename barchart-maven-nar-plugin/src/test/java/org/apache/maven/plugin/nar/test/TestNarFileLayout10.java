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

package org.apache.maven.plugin.nar.test;

import java.io.File;

import org.apache.maven.plugin.nar.Library;
import org.apache.maven.plugin.nar.NarFileLayout;
import org.apache.maven.plugin.nar.NarFileLayout10;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 * @version $Id$
 */
public class TestNarFileLayout10
    extends TestCase
{
    protected NarFileLayout fileLayout;

    protected String artifactId;

    protected String version;

    protected String aol;

    protected String type;

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        fileLayout = new NarFileLayout10();
        artifactId = "artifactId";
        version = "version";
        aol = "x86_64-MacOSX-g++";
        type = Library.SHARED;
    }

    public final void testGetIncludeDirectory()
    {
        Assert.assertEquals( "include", fileLayout.getIncludeDirectory() );
    }

    public final void testGetLibDirectory()
    {
        Assert.assertEquals( "lib" + File.separator + aol + File.separator + type, fileLayout.getLibDirectory( aol,
                                                                                                               type ) );
    }

    public final void testGetBinDirectory()
    {
        Assert.assertEquals( "bin" + File.separator + aol, fileLayout.getBinDirectory( aol ) );
    }
}
