/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.groovy.scripts;

import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * The default implementation of ScriptSourceMappingHandler.  This implementation stores
 * a mapping file in &lt;root_project_dir&gt;/.gradle/script-mapping.properties.  It maintains a basic
 * form of synchronization by monitoring the lastModified time of the file.  If the file has
 * changed, it's contents are re-read.
 *
 * @author John Murph
 */
public class DefaultScriptSourceMappingHandler implements ScriptSourceMappingHandler
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultScriptSourceMappingHandler.class);
    private static final String MAPPING_FILE_NAME = "script-mapping.properties";

    private final File mappingFile;
    private final Properties properties = new Properties();
    private long lastModifiedTime;
    private long lastLength;

    public DefaultScriptSourceMappingHandler(File buildDir)
    {
        File buildTmpDir = new File(buildDir, Project.TMP_DIR_NAME);
        mappingFile = new File(buildTmpDir, MAPPING_FILE_NAME);
    }

    /**
     * {@inheritDoc}
     */
    public File getSource(String buildScriptClassName)
    {
        if (buildScriptClassName == null)
            throw new NullPointerException();

        loadPropertiesIfNecessary();
        String path = properties.getProperty(buildScriptClassName);
        return path != null ? new File(path) : null;
    }

    /**
     * {@inheritDoc}
     */
    public void addSource(ScriptSource source)
    {
        File sourceFile = source.getSourceFile();
        if (sourceFile == null)
            return;

        loadPropertiesIfNecessary();
        String absoluteSourcePath = sourceFile.getAbsolutePath();
        String className = source.getClassName();
        String oldSource = properties.getProperty(className);
        if (oldSource == null || !absoluteSourcePath.equals(oldSource))
        {
            properties.setProperty(className, absoluteSourcePath);
            writeProperties();
        }
    }

    private void loadPropertiesIfNecessary()
    {
        if (mappingFile.lastModified() != lastModifiedTime || mappingFile.length() != lastLength)
        {
            lastModifiedTime = mappingFile.lastModified();
            lastLength = mappingFile.length();
            properties.clear();
            if (mappingFile.canRead())
            {
                try
                {
                    InputStream inputStream = new BufferedInputStream(new FileInputStream(mappingFile));
                    try
                    {
                        properties.load(inputStream);
                    }
                    finally
                    {
                        inputStream.close();
                    }
                }
                catch (IOException e)
                {
                    logger.error("Exception reading script mapping file", e);
                }
            }
        }
    }

    private void writeProperties()
    {
        File dir = mappingFile.getParentFile();
        if (!dir.exists())
            dir.mkdirs();
        
        if (mappingFile.canWrite() || (!mappingFile.exists() && dir.canWrite()))
        {
            try
            {
                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(mappingFile));
                try
                {
                    properties.store(outputStream, "Autogenerated.  Do not edit.");
                }
                finally
                {
                    outputStream.close();
                }
            }
            catch (IOException e)
            {
                logger.error("Exception writing script mapping file", e);
            }
        }
    }
}