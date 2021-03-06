package keggdbhelper.helpers;

import keggdbhelper.models.Compound;
import keggdbhelper.models.Entry;
import keggdbhelper.models.Pathway;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by Emilio on 2/19/16.
 */
public class OutputGenerator {

    private static final Logger log= Logger.getLogger( OutputGenerator.class.getName() );
    static {
        try {
            FileHandler fileHandler = new FileHandler(System.getProperty("user.dir") + "/OutputGenerator.log");
            log.addHandler(fileHandler);
            fileHandler.setFormatter(new SimpleFormatter());
            log.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static final String HEADER_TEMPLATE = "<h3>%s</h3>";
    public static final String LIST_TEMPLATE = "<ul>\n%s\n</ul>";
    private static final String FIND_TEMPLATE = "http://rest.kegg.jp/find/compound/%s";
    private static final String GET_TEMPLATE = "http://rest.kegg.jp/get/%s";
    private static final String CSS_STYLE_STRING = "" +
            "<head>\n" +
            "<style>\n" +
            "    h3 {\n" +
            "        padding: 0 0 0 0;\n" +
            "        margin: 0 0 0 0;\n" +
            "    }\n" +
            "\n" +
            "    ul {\n" +
            "        padding: 10 0 10 20;\n" +
            "        margin: 0 0 0 20;\n" +
            "    }\n" +
            "\n" +
            "    body {\n" +
            "        background-color: #FCFBE3;\n" +
            "    }\n" +
            "\n" +
            "    li {\n" +
            "        line-height: 1em;\n" +
            "    }\n" +
            "\n" +
            "    ul ul {\n" +
            "        margin: 0 0 0 0;\n" +
            "    }\n" +
            "</style>\n" +
            "</head>\n";


    public static boolean generateOutput(Collection<Compound> compounds, String outputFilename, Options options) {
        if (!retrieve(compounds, RetrievalThread.RetrievalType.ENTRY, options)) {
            log.log(Level.SEVERE, "retrieve IDs failed");
        }

        if (!retrieve(compounds, RetrievalThread.RetrievalType.PATHWAY, options)) {
            log.log(Level.SEVERE, "retrieve pathways failed");
        }

        if (options.shouldMapCompoundsToPathways) {
            if (!OutputWriter.writeToOutput(compounds, outputFilename, OutputWriter.OutputFormat.CtoP)) {
                log.log(Level.SEVERE, "write compounds to output failed");
                return false;
            }
        }

        if (options.shouldMapPathwaysToCompounds) {
            if (!OutputWriter.writeToOutput(compounds, outputFilename, OutputWriter.OutputFormat.PtoC)) {
                log.log(Level.SEVERE, "write pathways to output failed");
                return false;
            }
        }

        return true;
    }

    private static class OutputWriter {

        enum OutputFormat {
            CtoP,
            PtoC
        }

        public static boolean writeToOutput(Collection<Compound> compounds, String outputFilename, OutputFormat format) {
            switch (format) {
                case CtoP:
                    return writeCtoP(compounds, outputFilename);
                case PtoC:
                    return writePtoC(compounds, outputFilename);
                default:
                    log.log(Level.SEVERE, "unrecognized output format");
                    return false;
            }
        }

        private static boolean writeCtoP(Collection<Compound> compounds, String outputFilename) {
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(outputFilename + "-compounds.html", "UTF-8");
                List<Compound> compoundList = new ArrayList<>(compounds);
                Collections.sort(compoundList, (o1, o2) -> (o1.getName().compareTo(o2.getName())));
                writer.println(CSS_STYLE_STRING);

                for (Compound currentCompound : compoundList) {
                    log.log(Level.FINE, String.format("writing to output for compound %s\n", currentCompound.getName()));
                    writer.print(currentCompound.outputString());
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage());
                return false;
            } finally {
                try { writer.close(); } catch (Throwable ignore) {}
            }
            return true;
        }

        private static boolean writePtoC(Collection<Compound> compounds, String outputFilename) {
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(outputFilename + "-pathways.html", "UTF-8");
                Map<Pathway, Set<Compound>> pToCMap = new HashMap<>();
                for (Compound compound : compounds) {
                    for (Entry entry : compound.getEntries()) {
                        for (Pathway pathway : entry.getPathways()) {
                            if (!pToCMap.containsKey(pathway)) {
                                Set<Compound> newSet = new HashSet<>();
                                newSet.add(compound);
                                pToCMap.put(pathway, newSet);
                            } else {
                                pToCMap.get(pathway).add(compound);
                            }
                        }
                    }
                }
                writer.println(CSS_STYLE_STRING);

                List<Pathway> orderedList = new ArrayList<>(pToCMap.keySet());
                Collections.sort(orderedList, ((o1, o2) -> o1.getId().compareTo(o2.getId())));

                for (Pathway currentPathway : orderedList) {
                    log.log(Level.FINE, String.format("writing to output for pathway %s\n", currentPathway.getId()));
                    writer.println(currentPathway.headerString());
                    writer.println("<ul>");
                    for (Compound compound : pToCMap.get(currentPathway)) {
                        writer.printf("<li>%s</li>\n", compound.getName());
                    }
                    writer.println("</ul>");
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                try { writer.close(); } catch (Throwable ignore) {}
            }
            return true;
        }
    }



    public static boolean retrieve(Collection<Compound> compounds, RetrievalThread.RetrievalType retrievalType, Options options) {

        Collection<Thread> retrievalThreads = new LinkedList<>();

        for (Compound currentCompound : compounds) {
            Thread currentThread = new RetrievalThread(currentCompound, retrievalType, options);
            currentThread.start();
            retrievalThreads.add(currentThread);
        }

        for (Thread thread : retrievalThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                log.log(Level.WARNING, e.getMessage());
                return false;
            }
        }

        return true;
    }

    public static class Options {
        String organismCode;
        boolean shouldMapCompoundsToPathways;
        boolean shouldMapPathwaysToCompounds;

        public Options(String organismCode, boolean cToP, boolean pToC) {
            this.organismCode = organismCode;
            this.shouldMapCompoundsToPathways = cToP;
            this.shouldMapPathwaysToCompounds = pToC;
        }

        public String getOrganismCode() {
            return this.organismCode;
        }
    }

    private static class RetrievalThread extends Thread {

        enum RetrievalType {
            ENTRY,
            PATHWAY
        }

        Compound compound;
        RetrievalType retrievalType;
        Options options;

        RetrievalThread(Compound compound, RetrievalType retrievalType, Options options) {
            this.compound = compound;
            this.retrievalType = retrievalType;
            this.options = options;
        }

        @Override
        public void run() {
            switch (this.retrievalType) {
                case ENTRY:
                    runEntryRetrieval();
                    break;
                case PATHWAY:
                    runPathwayRetrieval();
                    break;
                default:
                    log.log(Level.SEVERE, "unknown retrieval type");
            }
        }

        private void runEntryRetrieval() {
            log.log(Level.FINE, String.format("retrieving IDs for compound %s\n", compound.getName()));
            String currentLine;
            List<Entry> entries = new LinkedList<>();
            String urlString = String.format(FIND_TEMPLATE, compound.getName());
            InputStream input = null;
            try {
                URL url = new URL(urlString);
                URLConnection connection = url.openConnection();
                input = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                while ((currentLine = reader.readLine()) != null) {
                    if (currentLine.isEmpty()) {
                        continue;
                    }
                    entries.add(new Entry(currentLine.split("\t")[0].replace("cpd:", "")));
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage());
            } finally {
                try { input.close(); } catch (Throwable ignore) {}
            }
            compound.setEntries(entries);
        }

        private void runPathwayRetrieval() {
            log.log(Level.FINE, String.format("retrieving pathways for compound %s\n", compound.getName()));
            if (compound.getEntries().isEmpty()) {
                return;
            }

            int i = 0;
            List<StringBuilder> builders = new LinkedList<>();
            StringBuilder queryStringBuilder = new StringBuilder();
            builders.add(queryStringBuilder);

            for (Entry entry : compound.getEntries()) {
                queryStringBuilder.append(entry.getId()).append('+');
                if (++i % 10 == 0) {
                    queryStringBuilder = new StringBuilder();
                    builders.add(queryStringBuilder);
                }
            }

            Iterator<Entry> entryIterator = compound.getEntries().iterator();

            for (StringBuilder sb : builders) {
                if (sb.length() == 0) {
                    continue;
                }

                int iterations = Math.min((i -= 10) + 10, 10);

                String urlString = String.format(GET_TEMPLATE, sb.toString());
                InputStream input = null;
                try {
                    URL url = new URL(urlString);
                    URLConnection connection = url.openConnection();
                    input = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                    for (int j = 0; j < iterations; ++j) {
                        parseDataIntoEntry(entryIterator.next(), reader);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try { input.close(); } catch (Throwable ignore) {}
                }
            }
        }

        private void parseDataIntoEntry(Entry entry, BufferedReader reader) throws IOException {
            reader.readLine();
            StringBuilder entryNameBuilder = new StringBuilder();
            String currentLine;
            List<Pathway> pathways = new LinkedList<>();

            entryNameBuilder.append(reader.readLine());
            while (!(currentLine = reader.readLine()).matches("[A-Z]+.*")) {
                entryNameBuilder.append(currentLine);
            }

            entry.setName(entryNameBuilder.toString().replace("NAME", ""));

            while (!(currentLine = reader.readLine()).contains("///")) {
                if (currentLine.isEmpty() || !currentLine.matches(".*\\bmap\\d{5}\\b.*")) {
                    continue;
                }

                if (!options.organismCode.isEmpty()) {
                    currentLine = currentLine.replaceAll("map", options.organismCode);
                }

                pathways.add(parseLineIntoPathway(currentLine));
            }

            entry.setPathways(pathways);
        }

        private static Pathway parseLineIntoPathway(String line) {
            List<String> components = new LinkedList<>();

            for (String string : line.split(" ")) {
                if (string.isEmpty() || string.contains("PATHWAY")) {
                    continue;
                }
                components.add(string);
            }

            String id = components.remove(0);
            StringBuilder nameBuilder = new StringBuilder();
            for (String nameComponent : components) {
                nameBuilder.append(nameComponent).append(' ');
            }

            return new Pathway(nameBuilder.toString(), id);
        }
    }
}
