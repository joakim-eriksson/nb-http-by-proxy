package se.jocke.nb.http.bp.core.decompress;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import org.openide.util.Exceptions;

public class GZipDecompressor implements Decompressor {

    @Override
    public String decompress(String data, String charSet) {
        try (ByteArrayInputStream bias = new ByteArrayInputStream(data.getBytes());
                GZIPInputStream gzis = new GZIPInputStream(bias)) {
            return new String(gzis.readAllBytes(), Charset.forName(charSet));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return data;
        }
    }

    @Override
    public String format() {
        return "gzip";
    }
}
