package eu.faircode.netguard;

import android.provider.Settings;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.Console;
import java.io.IOException;

/**
 * Created by Micha on 02.09.16.
 */
public class CategoryActivity {
    public void callURL() {
        try {
            System.out.println("Retrieving data from wikipedia.de");
            Document doc = Jsoup.connect("www.wikipedia.de").get();
            Elements newsHeadlines = doc.select("#mp-itn b a");
            System.out.println(newsHeadlines.html());

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Wikipedia cannot be called.");
        }


    }
}
