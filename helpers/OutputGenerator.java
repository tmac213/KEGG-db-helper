package keggdbhelper.helpers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emilio on 2/19/16.
 */
public class OutputGenerator {

    static final String FIND_TEMPLATE = "http://rest.kegg.jp/find/compound/%s";


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
