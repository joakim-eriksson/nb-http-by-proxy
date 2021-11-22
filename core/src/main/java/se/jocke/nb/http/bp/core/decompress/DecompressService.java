package se.jocke.nb.http.bp.core.decompress;

import java.util.List;

public interface DecompressService {

    String decompress(String data, List<String> encodings, String charSet);
}
