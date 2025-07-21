import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main {
    public static void main(String[] args) throws IOException {

        String option = args[0];
        String jarPath =  args[1];
        File file = new File(jarPath);
        Gson gson = new Gson();
        List<String> jarFileTypes = Arrays.asList(
                ".class",
                ".java",
                ".xml",
                ".yml",
                ".yaml",
                ".json",
                ".txt",
                ".properties",
                ".mf", // MANIFEST.MF
                ".png",
                ".jpg",
                ".jpeg",
                ".gif",
                ".svg",
                ".html",
                ".htm",
                ".css",
                ".js"
        );

        try(JarFile jarFile = new JarFile(file);) {
            Enumeration<JarEntry> entry = jarFile.entries();

            if(option.equals("classDecompile")){
                Path tempDir = Files.createTempDirectory("cfr_decompiled_");

                String outputDir = tempDir.toAbsolutePath().toString();
                String javaPath = args[2];
                String classPath =args[3];
                String cfrJarPath =args[4];
                while(entry.hasMoreElements()){
                    JarEntry jarEntry = entry.nextElement();
                    String[] str = jarEntry.getName().split("\\.");
                    if(jarFileTypes.contains("."+str[str.length-1].toLowerCase()) && jarEntry.getName().equals(classPath)) {
                        File classFile = new File(outputDir, jarEntry.getName());
                        classFile.getParentFile().mkdirs();
                        try (InputStream is = jarFile.getInputStream(jarEntry);
                             FileOutputStream fos = new FileOutputStream(classFile);
                        ) {
                            is.transferTo(fos);
                        }
                        boolean classFileFlag = jarEntry.getName().endsWith(".class");
                        // Build the CFR command to decompile the class file

                        ProcessBuilder pb = new ProcessBuilder(
                                javaPath, "-jar", cfrJarPath,
                                classFile.getAbsolutePath()
                        );
                        Process process = null;
                        pb.redirectErrorStream(true);
                        if(classFileFlag){
                            process = pb.start();
                        }

                        StringBuffer content = new StringBuffer("");
                        // Read and print any output (optional)
                        try {
                            BufferedReader reader = null;
                             reader = classFileFlag  ? new BufferedReader(
                                     new InputStreamReader(process.getInputStream())) : new BufferedReader(new FileReader(classFile)) ;
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.trim().startsWith("/*") || line.trim().startsWith("*") || line.trim().startsWith("*/")) {
                                    continue;
                                }
                                content.append(line).append("\n");
                            }
                        } catch (Exception e){
                            throw e;
                        }

                        int exitCode =classFileFlag ? process.waitFor() : 0 ;
                        if (exitCode == 0) {
                            System.out.println(Base64.getUrlEncoder().encodeToString(content.toString().getBytes()));

                        } else {
                            System.err.println("❌ Decompilation failed. Exit code: " + exitCode);
                        }
                    }
                }
                deleteTempDirectory(tempDir);
            }else if(option.equals("JarView")){

               List< Map<String,String> > entries = new ArrayList<>();
               while (entry.hasMoreElements()){
                   JarEntry jarEntry = entry.nextElement();
                   Map<String,String> mapEntry = new HashMap<>();
                   mapEntry.put("name",jarEntry.getName());
                   mapEntry.put("type",jarEntry.isDirectory()?"directory":"file");
                   entries.add(mapEntry);
               }

                System.out.println(gson.toJson(entries));

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public static void deleteTempDirectory(Path tempDir) throws IOException {
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder()) // delete children before parents
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + path + " due to " + e);
                        }
                    });
        }
    }
}