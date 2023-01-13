package fish.payara.advisor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.MalformedInputException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "advisor-tool", defaultPhase = LifecyclePhase.VERIFY)
public class AdvisorToolMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;


    @Override
    public void execute() {
        Properties patterns = null;
        Map<String, String> messages = null;
        try {
            //loadPatterns
            patterns = loadPatterns();
            //readSourceFiles
            List<File> files = readSourceFiles();
            if (patterns != null && !patterns.isEmpty() && !files.isEmpty()) {
                //searchPatterns
                messages = searchPatterns(patterns, files);
            }
            //print messages
            printMessages(messages);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Properties loadPatterns() throws URISyntaxException, IOException {
        URI uriBaseFolder = this.getClass().getClassLoader().getResource("mappedPatterns").toURI();
        Properties readPatterns = new Properties();
        Path internalPath = null;
        if(uriBaseFolder.getScheme().equals("jar")) {
            FileSystem fileSystem = FileSystems.newFileSystem(uriBaseFolder, Collections.<String, Object>emptyMap());
            internalPath = fileSystem.getPath("/mappedPatterns");
        } else {
            internalPath = Paths.get(uriBaseFolder);
        }
        if(internalPath != null) {
            Stream<Path> walk = Files.walk(internalPath, 1);
            for (Iterator<Path> it = walk.iterator(); it.hasNext();){
                Path p = it.next();
                if(p.getFileName().toString().contains(".properties")) {
                    readPatterns.load(this.getClass().getResourceAsStream(p.toString()));
                }
            }
        }
        return readPatterns;
    }

    public List<File> readSourceFiles() throws IOException {
        List<File> availableFiles = new ArrayList<>();
        if(project.getBasedir() != null) {
            availableFiles = Files.walk(Paths.get(project.getBasedir().toURI()))
                    .filter(Files::isRegularFile).filter( p -> !p.toString().contains(".class")
                            && !p.toString().contains(".war")
                            && !p.toString().contains(".jar")
                            && !p.toString().contains(".ear"))
                    .map(f -> f.toFile())
                    .collect(Collectors.toList());
        }
        return availableFiles;
    }

    public Map<String, String> searchPatterns(Properties patterns, List<File> files) throws IOException {
        Map<String, String> resultingMessages = new HashMap<>();
        for (File sourceFile: files) {
                List<String> lines = null;
                try {
                    lines = Files.readAllLines(sourceFile.toPath());
                } catch (MalformedInputException e) {
                    getLog().info("Error reading file:" + sourceFile);
                }
                if (lines!=null) {
                    for (String l : lines) {
                        patterns.forEach((key, value) -> {
                            //need to improve the use regex
                            //Pattern p = Pattern.compile((String) value);
                            //Matcher m = p.matcher(l);
                            String fileName = null;
                            String keyIssue = null;
                            String spec = null;
                            if (l.contains((String) value)) {
                                String k = (String) key;
                                String v = (String) value;
                                if (k.contains("method")) {
                                    spec = k.substring(0, k.indexOf("method"));
                                    fileName = spec + "messages.properties";
                                } else if (k.contains("remove")) {
                                    spec = k.substring(0, k.indexOf("remove"));
                                    fileName = spec + "messages.properties";
                                }

                                if (spec != null && k.contains("issue")) {
                                    keyIssue = spec + k.substring(k.indexOf("issue"), k.length());
                                }

                                if (fileName != null && keyIssue != null) {
                                    resultingMessages.put(keyIssue, fileName);
                                }
                            }
                        });
                    }
                }
            }
        return resultingMessages;
    }

    public void printMessages(Map<String, String> messages) throws URISyntaxException {
        getLog().info("Showing Advices");
        messages.forEach((k, v) -> {
            try {
                URI baseMessageFolder = this.getClass().getClassLoader().getResource("advisorMessages").toURI();
                Properties messageProperties = new Properties();
                Path internalPath = null;
                if (baseMessageFolder.getScheme().equals("jar")) {
                    FileSystem fileSystem = FileSystems.getFileSystem(baseMessageFolder);
                    internalPath = fileSystem.getPath("/advisorMessages");
                } else {
                    internalPath = Paths.get(baseMessageFolder);
                }
                if(internalPath != null) {
                    Stream<Path> walk = Files.walk(internalPath, 1);
                    for (Iterator<Path> it = walk.iterator(); it.hasNext();){
                        Path p = it.next();
                        if(p.getFileName().toString().contains((String)v)) {
                            messageProperties.load(this.getClass().getResourceAsStream(p.toString()));
                        }
                    }
                }
                getLog().info(messageProperties.getProperty(k));
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
