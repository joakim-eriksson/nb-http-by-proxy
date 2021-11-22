package se.jocke.nb.http.bp.core.service;

import io.undertow.server.ConduitWrapper;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.ConduitFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.xnio.conduits.AbstractStreamSourceConduit;
import org.xnio.conduits.StreamSourceConduit;

public class RequestConduitWrapper implements ConduitWrapper<StreamSourceConduit> {

    public static final AttachmentKey<byte[]> REQUEST = AttachmentKey.create(byte[].class);

    private final ByteArrayOutputStream outputStream;

    public RequestConduitWrapper(HttpServerExchange exchange) {
        long length = exchange.getRequestContentLength();
        if (length <= 0L) {
            outputStream = new ByteArrayOutputStream();
        } else {
            outputStream = new ByteArrayOutputStream((int) length);
        }
    }

    @Override
    public StreamSourceConduit wrap(ConduitFactory<StreamSourceConduit> factory, HttpServerExchange exchange) {
        StreamSourceConduit source = factory.create();
        return new AbstractStreamSourceConduit<StreamSourceConduit>(source) {
            @Override
            public int read(ByteBuffer dst) throws IOException {
                int x = super.read(dst);
                if (x >= 0) {
                    ByteBuffer dup = dst.duplicate();
                    dup.flip();
                    byte[] data = new byte[x];
                    dup.get(data);
                    outputStream.write(data);
                } else {
                    exchange.putAttachment(REQUEST, outputStream.toByteArray());
                }
                return x;
            }
        };
    }
}
