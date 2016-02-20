package keggdbhelper.helpers;

import keggdbhelper.models.Compound;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Created by Emilio on 2/19/16.
 */
public class OutputGenerator {

    static final String FIND_TEMPLATE = "http://rest.kegg.jp/find/compound/%s";

    public static boolean generateOutput(Collection<Compound> compounds) {
        if (!retrieveIDs(compounds)) {
            System.err.println("retrieve IDs failed");
        }

        /*if (!retrieveLinks(compounds)) {
            System.err.println("retrieve links failed");
        }*/

        if (!writeToOutput(compounds)) {
            System.err.println("write to output failed");
        }
        return true;
    }

    public static boolean writeToOutput(Collection<Compound> compounds) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("output.html", "UTF-8");
            List<Compound> compoundList = new ArrayList<>(compounds);
            Collections.sort(compoundList, (o1, o2) -> (o1.getName().compareTo(o2.getName())));
            writer.println(cssStyleString());

            for (Compound currentCompound : compoundList) {
                System.out.println(String.format("writing to output for compound %s", currentCompound.getName()));
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

    public static boolean retrieveIDs(Collection<Compound> compounds) {

        Collection<Thread> idRetrievalThreads = new LinkedList<>();

        for (Compound currentCompound : compounds) {
            Thread currentThread = new IDRetrievalThread(currentCompound);
            currentThread.start();
            idRetrievalThreads.add(currentThread);
        }

        for (Thread thread : idRetrievalThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private static class IDRetrievalThread extends Thread {
        Compound compound;

        IDRetrievalThread(Compound compound) {
            this.compound = compound;
        }

        @Override
        public void run() {
            System.out.println(String.format("retrieving IDs for compound %s", compound.getName()));
            String currentLine;
            ArrayList<String> ids = new ArrayList<>();
            String urlString = String.format(FIND_TEMPLATE, compound.getName());
            InputStream input = null;
            try {
                URL url = new URL(urlString);
                URLConnection conn = url.openConnection();
                input = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                if (input != null) {
                    while ((currentLine = reader.readLine()) != null) {
                        ids.add(currentLine.split("\t")[0].replace("cpd:", ""));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { input.close(); } catch (Throwable ignore) {}
            }
            compound.setIds(ids);
        }
    }

    private static boolean retrieveLinks(Collection<Compound> compounds) {


        return true;
    }

    private static void generateHTML() {

    }

    private static void appendTag(StringBuilder sb, String tag, String contents) {
        sb.append('<').append(tag).append('>');
        sb.append(contents);
        sb.append("</").append(tag).append('>');
    }

    private static String cssStyleString() {
        return "<head>\n" +
                "<style>\n" +
                "    h3 {\n" +
                "    padding: 0 0 0 0;\n" +
                "    margin: 0 0 0 0;\n" +
                "    }\n" +
                "\n" +
                "    ul {\n" +
                "    padding: 0 0 20 20;\n" +
                "    margin: 0 0 0 0;\n" +
                "    }\n" +
                "\n" +
                "    body {\n" +
                "        background-color: #FCFBE3;\n" +
                "    }\n" +
                "\n" +
                "    li {\n" +
                "        float: left;\n" +
                "    }\n" +
                "</style>\n" +
                "</head>\n";
    }

}
