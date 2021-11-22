package se.jocke.nb.http.bp.core.decompress;

public interface Decompressor {

    String decompress(String data, String charSet);
    
    String format();
}
