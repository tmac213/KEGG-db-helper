package keggdbhelper.helpers;

import keggdbhelper.models.Compound;
import keggdbhelper.models.Entry;
import keggdbhelper.models.Pathway;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Created by Emilio on 2/19/16.
 */
public class OutputGenerator {

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


    public static boolean generateOutput(Collection<Compound> compounds, String outputFilename) {
        if (!retrieve(compounds, RetrievalThread.RetrievalType.ENTRY)) {
            System.err.println("retrieve IDs failed");
        }

        if (!retrieve(compounds, RetrievalThread.RetrievalType.PATHWAY)) {
            System.err.println("retrieve pathways failed");
        }

        if (!writeToOutput(compounds, outputFilename)) {
            System.err.println("write to output failed");
        } else {
            System.out.println("generate output completed");
        }

        return true;
    }

    public static boolean writeToOutput(Collection<Compound> compounds, String outputFilename) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(outputFilename, "UTF-8");
            List<Compound> compoundList = new ArrayList<>(compounds);
            Collections.sort(compoundList, (o1, o2) -> (o1.getName().compareTo(o2.getName())));
            writer.println(CSS_STYLE_STRING);

            for (Compound currentCompound : compoundList) {
                System.out.printf("writing to output for compound %s\n", currentCompound.getName());
                writer.print(currentCompound.outputString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try { writer.close(); } catch (Throwable ignore) {}
        }
        return true;
    }

    public static boolean retrieve(Collection<Compound> compounds, RetrievalThread.RetrievalType retrievalType) {

        Collection<Thread> retrievalThreads = new LinkedList<>();

        for (Compound currentCompound : compounds) {
            Thread currentThread = null;
            currentThread = new RetrievalThread(currentCompound, retrievalType);
            currentThread.start();
            retrievalThreads.add(currentThread);
        }

        for (Thread thread : retrievalThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private static class RetrievalThread extends Thread {

        enum RetrievalType {
            ENTRY,
            PATHWAY
        }

        Compound compound;
        RetrievalType retrievalType;

        RetrievalThread(Compound compound, RetrievalType retrievalType) {
            this.compound = compound;
            this.retrievalType = retrievalType;
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
                    System.err.println("unknown retrieval type");
            }
        }

        private void runEntryRetrieval() {
            System.out.printf("retrieving IDs for compound %s\n", compound.getName());
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
                e.printStackTrace();
            } finally {
                try { input.close(); } catch (Throwable ignore) {}
            }
            compound.setEntries(entries);
        }

        private void runPathwayRetrieval() {
            System.out.printf("retrieving pathways for compound %s\n", compound.getName());
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

        private static void parseDataIntoEntry(Entry entry, BufferedReader reader) throws IOException {
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
