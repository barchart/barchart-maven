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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.CompilerDef;
import net.sf.antcontrib.cpptasks.LinkerDef;
import net.sf.antcontrib.cpptasks.OutputTypeEnum;
import net.sf.antcontrib.cpptasks.RuntimeType;
import net.sf.antcontrib.cpptasks.SubsystemEnum;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;
import net.sf.antcontrib.cpptasks.types.LinkerArgument;
import net.sf.antcontrib.cpptasks.types.SystemLibrarySet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Compiles native test source files.
 * 
 * @goal nar-testCompile
 * @phase test-compile
 * @requiresDependencyResolution test
 * @author Mark Donszelmann
 */
public class NarTestCompileMojo
    extends AbstractCompileMojo
{
    /**
     * Skip running of NAR integration test plugins.
     * 
     * @parameter expression="${skipNar}" default-value="false"
     */
    protected boolean skipNar;

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        // make sure destination is there
        getTestTargetDirectory().mkdirs();

        for ( Iterator i = getTests().iterator(); i.hasNext(); )
        {
            createTest( getAntProject(), (Test) i.next() );
        }
    }

    private void createTest( Project antProject, Test test )
        throws MojoExecutionException, MojoFailureException
    {
        String type = "test";

        // configure task
        CCTask task = new CCTask();
        task.setProject( antProject );

        // subsystem
        SubsystemEnum subSystem = new SubsystemEnum();
        subSystem.setValue( "console" );
        task.setSubsystem( subSystem );

        // outtype
        OutputTypeEnum outTypeEnum = new OutputTypeEnum();
        outTypeEnum.setValue( Library.EXECUTABLE );
        task.setOuttype( outTypeEnum );

        // outDir
        File outDir = new File( getTestTargetDirectory(), "bin" );
        outDir = new File( outDir, getAOL().toString() );
        outDir.mkdirs();

        // outFile
        File outFile = new File( outDir, test.getName() );
        getLog().debug( "NAR - output: '" + outFile + "'" );
        task.setOutfile( outFile );

        // object directory
        File objDir = new File( getTestTargetDirectory(), "obj" );
        objDir = new File( objDir, getAOL().toString() );
        objDir.mkdirs();
        task.setObjdir( objDir );

        // failOnError, libtool
        task.setFailonerror( failOnError( getAOL() ) );
        task.setLibtool( useLibtool( getAOL() ) );

        // runtime
        RuntimeType runtimeType = new RuntimeType();
        runtimeType.setValue( getRuntime( getAOL() ) );
        task.setRuntime( runtimeType );

        // add C++ compiler
        CompilerDef cpp = getCpp().getCompiler( type, test.getName() );
        if ( cpp != null )
        {
            task.addConfiguredCompiler( cpp );
        }

        // add C compiler
        CompilerDef c = getC().getCompiler( type, test.getName() );
        if ( c != null )
        {
            task.addConfiguredCompiler( c );
        }

        // add Fortran compiler
        CompilerDef fortran = getFortran().getCompiler( type, test.getName() );
        if ( fortran != null )
        {
            task.addConfiguredCompiler( fortran );
        }

        // add java include paths
        getJava().addIncludePaths( task, type );

        // add dependency include paths
        for ( Iterator i = getNarManager().getNarDependencies( "test" ).iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            
            // check if it exists in the normal unpack directory
            File include = 
                getLayout().getIncludeDirectory( getUnpackDirectory(), artifact.getArtifactId(), artifact.getVersion() );
            if ( !include.exists() )
            {
                // otherwise try the test unpack directory
                include = 
                    getLayout().getIncludeDirectory( getTestUnpackDirectory(), artifact.getArtifactId(), artifact.getVersion() );
            }
            if ( include.exists() )
            {                
                task.createIncludePath().setPath( include.getPath() );
            }
        }
        
        // add javah generated include path
        File jniIncludeDir = getJavah().getJniDirectory();
        if (jniIncludeDir.exists()) {
        	task.createIncludePath().setPath(jniIncludeDir.getPath());
        }

        // add linker
        LinkerDef linkerDefinition =
            getLinker().getLinker( this, antProject, getOS(), getAOL().getKey() + ".linker.", type );
        task.addConfiguredLinker( linkerDefinition );

        // FIXME hardcoded values
        String libName = getFinalName();
        File includeDir =
            getLayout().getIncludeDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                             getMavenProject().getVersion() );

        File libDir =
            getLayout().getLibDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                         getMavenProject().getVersion(), getAOL().toString(), test.getLink() );

        // copy shared library
        // FIXME why do we do this ?
        /*
         * Removed in alpha-10 if (test.getLink().equals(Library.SHARED)) { try { // defaults are Unix String libPrefix
         * = NarUtil.getDefaults().getProperty( getAOLKey() + "shared.prefix", "lib"); String libExt =
         * NarUtil.getDefaults().getProperty( getAOLKey() + "shared.extension", "so"); File copyDir = new
         * File(getTargetDirectory(), (getOS().equals( "Windows") ? "bin" : "lib") + "/" + getAOL() + "/" +
         * test.getLink()); FileUtils.copyFileToDirectory(new File(libDir, libPrefix + libName + "." + libExt),
         * copyDir); if (!getOS().equals(OS.WINDOWS)) { libDir = copyDir; } } catch (IOException e) { throw new
         * MojoExecutionException( "NAR: Could not copy shared library", e); } }
         */
        // FIXME what about copying the other shared libs?

        // add include of this package
        if ( includeDir.exists() )
        {
            task.createIncludePath().setLocation( includeDir );
        }

        // add library of this package
        if ( libDir.exists() )
        {
            LibrarySet libSet = new LibrarySet();
            libSet.setProject( antProject );
            libSet.setLibs( new CUtil.StringArrayBuilder( libName ) );
            LibraryTypeEnum libType = new LibraryTypeEnum();
            libType.setValue( test.getLink() );
            libSet.setType( libType );
            libSet.setDir( libDir );
            task.addLibset( libSet );
        }

        // add dependency libraries
        List depLibOrder = getDependencyLibOrder();
        List depLibs = getNarManager().getNarDependencies( "test" );

        // reorder the libraries that come from the nar dependencies
        // to comply with the order specified by the user
        if ( ( depLibOrder != null ) && !depLibOrder.isEmpty() )
        {

            List tmp = new LinkedList();

            for ( Iterator i = depLibOrder.iterator(); i.hasNext(); )
            {

                String depToOrderName = (String) i.next();

                for ( Iterator j = depLibs.iterator(); j.hasNext(); )
                {

                    NarArtifact dep = (NarArtifact) j.next();
                    String depName = dep.getGroupId() + ":" + dep.getArtifactId();

                    if ( depName.equals( depToOrderName ) )
                    {

                        tmp.add( dep );
                        j.remove();
                    }
                }
            }

            tmp.addAll( depLibs );
            depLibs = tmp;
        }

        for ( Iterator i = depLibs.iterator(); i.hasNext(); )
        {
            NarArtifact dependency = (NarArtifact) i.next();

            // FIXME no handling of "local"

            // FIXME, no way to override this at this stage
            String binding = dependency.getNarInfo().getBinding( getAOL(), Library.NONE );
            getLog().debug( "Using Binding: " + binding );
            AOL aol = getAOL();
            aol = dependency.getNarInfo().getAOL( getAOL() );
            getLog().debug( "Using Library AOL: " + aol.toString() );

            if ( !binding.equals( Library.JNI ) && !binding.equals( Library.NONE ) )
            {
                // check if it exists in the normal unpack directory 
                File dir =
                    getLayout().getLibDirectory( getUnpackDirectory(), dependency.getArtifactId(),
                                                  dependency.getVersion(), aol.toString(), binding );
                getLog().debug( "Looking for Library Directory: " + dir );
                if ( !dir.exists() )
                {
                    getLog().debug( "Library Directory " + dir + " does NOT exist." );

                    // otherwise try the test unpack directory
                    dir = getLayout().getLibDirectory( getTestUnpackDirectory(), dependency.getArtifactId(),
                                                        dependency.getVersion(), aol.toString(), binding );
                    getLog().debug( "Looking for Library Directory: " + dir );
                }
                if ( dir.exists() )
                {
                    LibrarySet libSet = new LibrarySet();
                    libSet.setProject( antProject );

                    // FIXME, no way to override
                    String libs = dependency.getNarInfo().getLibs( getAOL() );
                    if ( ( libs != null ) && !libs.equals( "" ) )
                    {
                        getLog().debug( "Using LIBS = " + libs );
                        libSet.setLibs( new CUtil.StringArrayBuilder( libs ) );
                        libSet.setDir( dir );
                        task.addLibset( libSet );
                    }
                }
                else
                {
                    getLog().debug( "Library Directory " + dir + " does NOT exist." );
                }

                // FIXME, look again at this, for multiple dependencies we may need to remove duplicates
                String options = dependency.getNarInfo().getOptions( getAOL() );
                if ( ( options != null ) && !options.equals( "" ) )
                {
                    getLog().debug( "Using OPTIONS = " + options );
                    LinkerArgument arg = new LinkerArgument();
                    arg.setValue( options );
                    linkerDefinition.addConfiguredLinkerArg( arg );
                }

                String sysLibs = dependency.getNarInfo().getSysLibs( getAOL() );
                if ( ( sysLibs != null ) && !sysLibs.equals( "" ) )
                {
                    getLog().debug( "Using SYSLIBS = " + sysLibs );
                    SystemLibrarySet sysLibSet = new SystemLibrarySet();
                    sysLibSet.setProject( antProject );

                    sysLibSet.setLibs( new CUtil.StringArrayBuilder( sysLibs ) );
                    task.addSyslibset( sysLibSet );
                }
            }
        }

        // Add JVM to linker
        getJava().addRuntime( task, getJavaHome( getAOL() ), getOS(), getAOL().getKey() + ".java." );

        // execute
        try
        {
            task.execute();
        }
        catch ( BuildException e )
        {
            throw new MojoExecutionException( "NAR: Test-Compile failed", e );
        }
    }

}
