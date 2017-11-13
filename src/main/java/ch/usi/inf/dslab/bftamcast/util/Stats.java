package ch.usi.inf.dslab.bftamcast.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class Stats {
    private Vector<Long> values;
    private int accCount, limit;

    /**
     * Creates a new instance of Stats
     */
    public Stats() {
        values = new Vector<>();
        accCount = 0;
    }

    public int getCount() {
        return values.size();
    }

    public void reset() {
        values.clear();
    }

    public void store(long value) {
        values.add(value);
        accCount++;
    }

    public int getPartialCount() {
        int temp = accCount;
        accCount = 0;
        return temp;
    }

    public long getMax(int discardPercent) {
        Vector<Long> values = new Vector<>(this.values);
        int limit = discardPercent * values.size() / 100;
        Collections.sort(values);

        return values.get(values.size() - 1 - limit);
    }

    public double getAverage(int discardPercent) {
        Vector<Long> values = new Vector<>(this.values);
        int size = values.size();
        double sum = 0;

        //Collections.sort(values);
        limit = discardPercent * size / 100;
        if (size < 2 * limit)
            limit = 0;

        for (int i = limit; i < size - limit; i++) {
            sum = sum + values.get(i);
        }
        return sum / (size - 2 * limit);
    }

    public double getStdDev(int discardPercent) {
        Vector<Long> values = new Vector<>(this.values);
        int size = values.size();
        double avg = getAverage(discardPercent), var, quad = 0;
        long num = size - 2 * limit;

        //Collections.sort(values);
        for (int i = limit; i < size - limit; i++)
            quad += ((values.get(i) - avg) * (values.get(i) - avg));

        var = quad / (num - 1);
        return Math.sqrt(var);
    }

    public long getPercentile(int percentile, int discardPercent) {
        Vector<Long> values = new Vector<>(this.values);
        int size = values.size(), index;
        float percent = (float) percentile / 100;

        Collections.sort(values);
        limit = discardPercent * size / 100;
        if (size < 2 * limit)
            limit = 0;


        index = limit + Math.round(percent * (size - 2 * limit));
        if (index >= size)
            index = size - 1;

        return values.get(index);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public void persist(String fileName, int discardPercent) {
        File f = new File(fileName);
        long abs = 0;
        int order = 0;
        try {
            FileWriter fw = new FileWriter(f);
            fw.write("ORDER\tLATENCY\tABS\n");
            for (long value : values) {
                abs += value;
                fw.write(++order + "\t" + value + "\t" + abs + "\n");
            }
            fw.write("\n");
            fw.write(toString(discardPercent));
            fw.flush();
            fw.close();
        } catch (IOException ex) {
            System.err.println("Unable to save stats to file");
            ex.printStackTrace();
        }
    }

    private String toString(int discardPercent) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n--------------\nStats:\n");
        sb.append("\tTotal: " + getCount() + "\n");
        sb.append("\tMean: " + getAverage(discardPercent) + "\n");
        sb.append("\tStdDev: " + getStdDev(discardPercent) + "\n");
        sb.append("\tMax: " + getMax(discardPercent) + "\n");
        sb.append("\tPercentile (25, 50, 75, 95, 99): (" + getPercentile(25, discardPercent) + ", " +
                getPercentile(50, discardPercent) + ", " + getPercentile(75, discardPercent) + ", " +
                getPercentile(95, discardPercent) + ", " + getPercentile(99, discardPercent) + ")\n--------------\n\n");
        return sb.toString();
    }
}


