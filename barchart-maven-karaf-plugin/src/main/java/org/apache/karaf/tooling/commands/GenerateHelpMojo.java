/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.commands;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.karaf.shell.commands.Action;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.HelpOption;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.commands.meta.ActionMetaData;
import org.apache.karaf.shell.commands.meta.ActionMetaDataFactory;
import org.apache.karaf.shell.util.ShellUtil;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.xbean.finder.ClassFinder;

/**
 * Generates help documentation for Karaf commands
 *
 * @goal commands-generate-help
 * @phase generate-resources
 * @execute phase="generate-resources"
 * @requiresDependencyResolution runtime
 * @inheritByDefault false
 * @description Generates help for Karaf commands
 */
public class GenerateHelpMojo extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     */
    protected MavenProject project;

    /**
     * The output folder
     *
     * @parameter default-value="${project.build.directory}/docbkx/sources"
     */
    protected File targetFolder;

    /**
     * The output format
     *
     * @parameter default-value="docbx";
     */
    protected String format;

    /**
     * The classloader to use for loading the commands
     *
     * @parameter default-value="project"
     */
    protected String classLoader;

    /**
     * Includes the --help command output in the generated documentation
     *
     * @parameter default_value="true"
     */
    protected boolean includeHelpOption;

    private static final String FORMAT_CONF = "conf";
    private static final String FORMAT_DOCBX = "docbx";

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (!FORMAT_DOCBX.equals(format) && !FORMAT_CONF.equals(format)) {
                throw new MojoFailureException("Unsupported format: " + format + ". Supported formats are: docbx or conf.");
            }
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }

            ClassFinder finder;
            if ("project".equals(classLoader)) {
                List<URL> urls = new ArrayList<URL>();
                for (Object object : project.getCompileClasspathElements()) {
                    String path = (String) object;
                    urls.add(new File(path).toURI().toURL());
                }
                ClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
                finder = new ClassFinder(loader, urls);
            } else if ("plugin".equals(classLoader)) {
                finder = new ClassFinder(getClass().getClassLoader());
            } else {
                throw new MojoFailureException("classLoader attribute must be 'project' or 'plugin'");
            }
            List<Class<?>> classes = finder.findAnnotatedClasses(Command.class);
            if (classes.isEmpty()) {
                throw new MojoFailureException("No command found");
            }

            Map<String, Set<String>> commands = new TreeMap<String, Set<String>>();

            for (Class clazz : classes) {
                try {
                    Action action = (Action) clazz.newInstance();
                    ActionMetaData meta = new ActionMetaDataFactory().create(action.getClass());
                    String help = printHelp(action, meta, format, includeHelpOption);
                    Command cmd = meta.getCommand();
                    File output = null;

                    // skip the *-help command
                    if (cmd.scope().equals("*")) continue;

                    if (FORMAT_DOCBX.equals(format)) {
                        output = new File(targetFolder, cmd.scope() + "-" + cmd.name() + ".xml");
                    } else if (FORMAT_CONF.equals(format)) {
                        output = new File(targetFolder, cmd.scope() + "-" + cmd.name() + ".conf");
                    }
                    Writer writer = new OutputStreamWriter(new FileOutputStream(output));
                    writer.write(help);
                    writer.close();

                    Set<String> cmds = commands.get(cmd.scope());
                    if (cmds == null) {
                        cmds = new TreeSet<String>();
                        commands.put(cmd.scope(), cmds);
                    }
                    cmds.add(cmd.name());
                    getLog().info("Found command: " + cmd.scope() + ":" + cmd.name());
                } catch (Exception e) {
                    getLog().warn("Unable to write help for " + clazz.getName(), e);
                }
            }

            if (FORMAT_DOCBX.equals(format)) {
                PrintStream writer = new PrintStream(new FileOutputStream(new File(targetFolder, "commands.xml")));
                writer.println("<chapter id='commands' xmlns:xi=\"http://www.w3.org/2001/XInclude\">");
                writer.println("  <title>Commands</title>");
                writer.println("  <toc></toc>");
                for (String key : commands.keySet()) {
                    writer.println("  <section id='commands-" + key + "'>");
                    writer.println("    <title>" + key + "</title>");
                    for (String cmd : commands.get(key)) {
                        writer.println("    <xi:include href='" + key + "-" + cmd + ".xml' parse='xml'/>");
                    }
                    writer.println("  </section>");
                }
                writer.println("</chapter>");
                writer.close();
            } else if (FORMAT_CONF.equals(format)) {
                PrintStream writer = new PrintStream(new FileOutputStream(new File(targetFolder, "commands.conf")));
                writer.println("h1. Commands");
                writer.println();
                for (String key : commands.keySet()) {
                    writer.println("h2. " + key);
                    writer.println();
                    for (String cmd : commands.get(key)) {
                        writer.println("* [" + key + ":" + cmd + "|" + key + "-" + cmd + "]");
                    }
                    writer.println();
                }
                writer.close();
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Error building commands help", e);
        }
    }

    public String printHelp(Action action, ActionMetaData meta, String format, boolean includeHelpOption) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream newout = new PrintStream(baos);
        
        if (FORMAT_DOCBX.equals(format)) {
            printUsageDocBook(action, meta, newout, includeHelpOption);
        } else {
            printUsageConf(action, meta, newout, includeHelpOption);
        }

        meta.printUsage(action, newout, false, 80);
        newout.close();
        baos.close();
        return baos.toString();
    }

    private void printUsageConf(Action action, ActionMetaData actionMeta, PrintStream out, boolean includeHelpOption) {
        Map<Option, Field> optionsMap = actionMeta.getOptions();
        Map<Argument, Field> argsMap = actionMeta.getArguments();
        Command command = action.getClass().getAnnotation(Command.class);
        List<Argument> arguments = new ArrayList<Argument>(argsMap.keySet());
        Collections.sort(arguments, new Comparator<Argument>() {
            public int compare(Argument o1, Argument o2) {
                return Integer.valueOf(o1.index()).compareTo(Integer.valueOf(o2.index()));
            }
        });
        Set<Option> options = new HashSet<Option>(optionsMap.keySet());
        if (includeHelpOption)
            options.add(HelpOption.HELP);

        out.println("<section>");
        out.print("  <title>");
        out.print(command.scope());
        out.print(":");
        out.print(command.name());
        out.println("</title>");
        out.println("  <section>");
        out.println("    <title>Description</title>");
        out.println("    <para>");
        out.println(command.description());
        out.println("    </para>");
        out.println("  </section>");

        StringBuffer syntax = new StringBuffer();
        syntax.append(String.format("%s:%s", command.scope(), command.name()));
        if (options.size() > 0) {
            syntax.append(" [options]");
        }
        if (arguments.size() > 0) {
            syntax.append(' ');
            for (Argument argument : arguments) {
                syntax.append(String.format(argument.required() ? "%s " : "[%s] ", argument.name()));
            }
        }
        out.println("  <section>");
        out.println("    <title>Syntax</title>");
        out.println("    <para>");
        out.println(syntax.toString());
        out.println("    </para>");
        out.println("  </section>");

        if (arguments.size() > 0) {
            out.println("  <section>");
            out.println("    <title>Arguments</title>");
            out.println("    <informaltable>");
            for (Argument argument : arguments) {
                out.println("    <tr>");
                out.println("      <td>" + argument.name() + "</td>");
                String description = argument.description();
                if (!argument.required()) {
                    try {
                        argsMap.get(argument).setAccessible(true);
                        Object o = argsMap.get(argument).get(action);
                        if (o != null && (!(o instanceof Boolean) || ((Boolean) o))
                                && (!(o instanceof Number) || ((Number) o).doubleValue() != 0.0)) {
                            description += " (defaults to " + o.toString() + ")";
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                out.println("      <td>" + description + "</td>");
                out.println("    </tr>");
            }

            out.println("    </informaltable>");
            out.println("  </section>");
        }
        if (options.size() > 0) {
            out.println("  <section>");
            out.println("    <title>Options</title>");
            out.println("    <informaltable>");

            for (Option option : options) {
                String opt = option.name();
                String description = option.description();
                for (String alias : option.aliases()) {
                    opt += ", " + alias;
                }
                try {
                    optionsMap.get(option).get(action);
                    Object o = optionsMap.get(option).get(action);
                    if (o != null && (!(o instanceof Boolean) || ((Boolean) o))
                            && (!(o instanceof Number) || ((Number) o).doubleValue() != 0.0)) {
                        description += " (defaults to " + o.toString() + ")";
                    }

                } catch (Exception e) {

                }
                out.println("    <tr>");
                out.println("      <td>" + opt + "</td>");
                out.println("      <td>" + description + "</td>");
                out.println("    </tr>");
            }

            out.println("    </informaltable>");
            out.println("  </section>");

            if (command.detailedDescription() != null && command.detailedDescription().trim().length() > 0) {
                out.println("<section>");
                out.println("   <title>Details</title>");
                String description = loadDescription(action.getClass(), command.detailedDescription());
                out.println("   <para>");

            }
        }
        out.println("</section>");
    }

    private void printUsageDocBook(Action action, ActionMetaData actionMeta, PrintStream out, boolean includeHelpOption) {
        Map<Option, Field> optionsMap = actionMeta.getOptions();
        Map<Argument, Field> argsMap = actionMeta.getArguments();
        Command command = actionMeta.getCommand();
        List<Argument> arguments = new ArrayList<Argument>(argsMap.keySet());
        Collections.sort(arguments, new Comparator<Argument>() {
            public int compare(Argument o1, Argument o2) {
                return Integer.valueOf(o1.index()).compareTo(Integer.valueOf(o2.index()));
            }
        });
        Set<Option> options = new HashSet<Option>(optionsMap.keySet());
        if (includeHelpOption)
            options.add(HelpOption.HELP);

        out.println("h1. " + command.scope() + ":" + command.name());
        out.println();

        out.println("h2. Description");
        out.println(command.description());
        out.println();

        StringBuffer syntax = new StringBuffer();
        syntax.append(String.format("%s:%s", command.scope(), command.name()));
        if (options.size() > 0) {
            syntax.append(" \\[options\\]");
        }
        if (arguments.size() > 0) {
            syntax.append(' ');
            for (Argument argument : arguments) {
                syntax.append(String.format(argument.required() ? "%s " : "\\[%s\\] ", argument.name()));
            }
        }
        out.println("h2. Syntax");
        out.println(syntax.toString());
        out.println();

        if (arguments.size() > 0) {
            out.println("h2. Arguments");
            out.println("|| Name || Description ||");
            for (Argument argument : arguments) {
                String description = argument.description();
                if (!argument.required()) {
                    try {
                        argsMap.get(argument).setAccessible(true);
                        Object o = argsMap.get(argument).get(action);
                        if (o != null && (!(o instanceof Boolean) || ((Boolean) o))
                                && (!(o instanceof Number) || ((Number) o).doubleValue() != 0.0)) {
                            description += " (defaults to " + o.toString() + ")";
                        }
                    } catch (Throwable t) {
                        // Ignore
                    }
                }
                out.println("| " + argument.name() + " | " + description + " |");
            }
            out.println();
        }
        if (options.size() > 0) {
            out.println("h2. Options");
            out.println("|| Name || Description ||");
            for (Option option : options) {
                String opt = option.name();
                String desc = option.description();
                for (String alias : option.aliases()) {
                    opt += ", " + alias;
                }
                try {
                    optionsMap.get(option).setAccessible(true);
                    Object o = optionsMap.get(option).get(action);
                    if (o != null && (!(o instanceof Boolean) || ((Boolean) o))
                            && (!(o instanceof Number) || ((Number) o).doubleValue() != 0.0)) {
                        desc += " (defaults to " + o.toString() + ")";
                    }
                } catch (Throwable t) {
                    // Ignore
                }
                out.println("| " + opt + " | " + desc + " |");
            }
            out.println();
        }
        if (command.detailedDescription().length() > 0) {
            out.println("h2. Details");
            String desc = loadDescription(action.getClass(), command.detailedDescription());
            out.println(desc);
        }
        out.println();
    }

    private String loadDescription(Class clazz, String desc) {
        if (desc.startsWith("classpath:")) {
            desc = ShellUtil.loadClassPathResource(clazz, desc.substring("classpath:".length()));
        }
        return desc;
    }
            
}
