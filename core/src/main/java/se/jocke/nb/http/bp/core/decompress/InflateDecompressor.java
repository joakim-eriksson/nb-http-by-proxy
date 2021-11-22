package se.jocke.nb.http.bp.core.decompress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.openide.util.Exceptions;

public class InflateDecompressor implements Decompressor {

    @Override
    public String decompress(String data, String charset) {
        Inflater inflater = new Inflater();
        inflater.setInput(data.getBytes());

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length())) {
            byte[] buffer = new byte[1024];

            while (!inflater.finished()) {
                int inflated = inflater.inflate(buffer);
                outputStream.write(buffer, 0, inflated);
            }

            return outputStream.toString(charset);
        } catch (IOException | DataFormatException ex) {
            Exceptions.printStackTrace(ex);
        }

        return data;
    }

    @Override
    public String format() {
        return "inflate";
    }
}
