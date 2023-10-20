package dslab;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

import dslab.mailbox.IMailboxServer;
import dslab.mailbox.MailboxServer;
import dslab.monitoring.IMonitoringServer;
import dslab.monitoring.MonitoringServer;
import dslab.transfer.ITransferServer;
import dslab.transfer.TransferServer;
import dslab.util.Config;

/**
 * The component factory provides methods to create the core components of the application. You can edit the method body
 * if the component instantiation requires additional logic.
 *
 * Do not change the existing method signatures!
 */
public final class ComponentFactory {

    private ComponentFactory() {
        // static utility class
    }

    /**
     * Creates a new {@link IMonitoringServer} instance.
     *
     * @param componentId the component id
     * @param in the input stream used for accepting cli commands
     * @param out the output stream to print to
     * @return a new MonitoringServer instance
     */
    public static IMonitoringServer createMonitoringServer(String componentId, InputStream in, PrintStream out)
            throws Exception {
        /*
         * TODO: Here you can modify the code (if necessary) to instantiate your components
         */

        Config config = new Config(componentId);
        return new MonitoringServer(componentId, config, in, out);
    }

    /**
     * Creates a new {@link IMailboxServer} instance.
     *
     * @param componentId the component id
     * @param in the input stream used for accepting cli commands
     * @param out the output stream to print to
     * @return a new MailboxServer instance
     */
    public static IMailboxServer createMailboxServer(String componentId, InputStream in, PrintStream out)
            throws Exception {
        /*
         * TODO: Here you can modify the code (if necessary) to instantiate your components
         */

        Config config = new Config(componentId);
        return new MailboxServer(componentId, config, in, out);
    }

    /**
     * Creates a new {@link ITransferServer} instance.
     *
     * @param componentId the component id
     * @param in the input stream used for accepting cli commands
     * @param out the output stream to print to
     * @return a new TransferServer instance
     */
    public static ITransferServer createTransferServer(String componentId, InputStream in, PrintStream out)
            throws Exception {
        /*
         * TODO: Here you can modify the code (if necessary) to instantiate your components
         */

        Config config = new Config(componentId);

        Properties props = loadPropertiesFile(componentId);
        int portNr = Integer.parseInt(props.getProperty("tcp.port"));

        return new TransferServer(componentId, config, in, out, portNr);
    }

    public static Properties loadPropertiesFile(String fileName) {
        Properties properties = new Properties();

        try {
            ClassLoader classLoader = ComponentFactory.class.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(fileName + ".properties");

            if (inputStream != null) {
                properties.load(inputStream);
                inputStream.close();
            } else {
                System.err.println("Properties file not found: " + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    return properties;
    }
}
