package se.jocke.nb.http.bp.core.window;

import java.util.Collection;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import se.jocke.nb.http.bp.core.HttpExchange;
import se.jocke.nb.http.bp.core.node.HttpExchangeNode;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//se.jocke.nb.http.bp.core.window//HttpProperties//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "HttpPropertiesTopComponent",
        iconBase = "se/jocke/nb/http/bp/core/window/Network-Domain-icon.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "rightSlidingSide", openAtStartup = false)
@ActionID(category = "Window", id = "se.jocke.nb.http.bp.core.window.HttpPropertiesTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_HttpPropertiesAction",
        preferredID = "HttpPropertiesTopComponent"
)
@Messages({
    "CTL_HttpPropertiesAction=Exchange Properties",
    "CTL_HttpPropertiesTopComponent=Exchange Properties",
    "HINT_HttpPropertiesTopComponent=Exchange Properties"
})
public final class ExchangePropertiesTopComponent extends TopComponent implements LookupListener {

    private Lookup.Result<HttpExchange> result;

    public ExchangePropertiesTopComponent() {
        initComponents();
        setName(Bundle.CTL_HttpPropertiesTopComponent());
        setToolTipText(Bundle.HINT_HttpPropertiesTopComponent());
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        propsPanel = new PropertySheet();

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(propsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(propsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel propsPanel;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        Lookup.Template<HttpExchange> tpl = new Lookup.Template<>(HttpExchange.class);
        result = Utilities.actionsGlobalContext().lookup(tpl);
        result.addLookupListener(this);
    }

    @Override
    public void componentClosed() {
        result.removeLookupListener(this);
    }

    void writeProperties(java.util.Properties p) {
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
    }

    @Override
    public void resultChanged(LookupEvent le) {
        Lookup.Result<HttpExchange> r = (Lookup.Result) le.getSource();
        Collection<? extends HttpExchange> allInstances = r.allInstances();
        PropertySheet kps = (PropertySheet) propsPanel;

        if (!allInstances.isEmpty()) {

            HttpExchange exchange = allInstances.iterator().next();
            if (exchange == HttpExchange.EMPTY) {
                kps.setNodes(new Node[]{HttpExchangeNode.EMPTY_EXCHANGE_NODE});
            } else {
                kps.setNodes(new Node[]{new HttpExchangeNode(exchange)});
            }
        }
    }
}