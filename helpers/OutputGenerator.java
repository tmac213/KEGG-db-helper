package keggdbhelper.helpers;

import keggdbhelper.models.Compound;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Emilio on 2/19/16.
 */
public class OutputGenerator {

    static final String FIND_TEMPLATE = "http://rest.kegg.jp/find/compound/%s";

    public static boolean generateOutput(Collection<Compound> compounds) {
        if (!retrieveIDs(compounds)) {
            System.err.println("retrieve IDs failed");
        }

        if (!writeToOutput(compounds)) {
            System.err.println("write to output failed");
        }
        return true;
    }

    public static boolean writeToOutput(Collection<Compound> compounds) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("output.txt", "UTF-8");
            for (Compound currentCompound : compounds) {
                System.out.println(String.format("writing to output for compound %s", currentCompound.getName()));
                writer.println(currentCompound.getName());
                for (String currentID : currentCompound.getids()) {
                    writer.print(currentID);
                    writer.print(';');
                }
                writer.println();
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

        for (Compound currentCompound : compounds) {
            System.out.println(String.format("retrieving IDs for compound %s", currentCompound.getName()));
            String currentLine;
            ArrayList<String> ids = new ArrayList<>();
            String urlString = String.format(FIND_TEMPLATE, currentCompound.getName());
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
                return false;
            } finally {
                try { input.close(); } catch (Throwable ignore) {}
            }
            currentCompound.setIds(ids);
        }
        return true;
    }

    public static List<String> getIDs(String name) {
        String currentLine = null;
        ArrayList<String> results = new ArrayList<String>();
        String urlString = String.format(FIND_TEMPLATE, name);
        InputStream input = null;
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            input = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            if (input != null) {
                while ((currentLine = reader.readLine()) != null) {
                    results.add(currentLine.split("\t")[0].replace("cpd:", ""));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { input.close(); } catch (Throwable ignore) {}
        }

        return results;
    }


}
