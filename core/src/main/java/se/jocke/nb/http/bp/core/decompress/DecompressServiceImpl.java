package se.jocke.nb.http.bp.core.decompress;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = DecompressService.class)
public class DecompressServiceImpl implements DecompressService {

    private static final Logger LOG = Logger.getLogger(DecompressServiceImpl.class.getName());

    private final Map<String, Decompressor> decompressors = new ConcurrentHashMap<>();

    public DecompressServiceImpl() {
        Lookup.getDefault().lookupAll(Decompressor.class)
                .forEach(decomp -> decompressors.put(decomp.format(), decomp));
    }

    @Override
    public String decompress(String data, List<String> encodings, String charSet) {
        String decompressed = data;
        if (decompressors.keySet().containsAll(encodings)) {
            //Hm, how would a stream soluton look like? 
            for (String encoding : encodings) {
                decompressed = decompressors.get(encoding).decompress(decompressed, charSet);
            }

            return decompressed;
        } else {
            LOG.log(Level.WARNING, "Not possible to decomress using encodings {0}", encodings);
        }

        return data;
    }
}
