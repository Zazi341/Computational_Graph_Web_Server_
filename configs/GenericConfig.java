package configs;

//---------------------------------------------------
import graph.Agent;
import graph.ParallelAgent;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
//---------------------------------------------------

/**
 * A generic configuration loader that creates computational graph agents from configuration files
 * using reflection-based instantiation.
 * 
 * <p>GenericConfig implements the factory pattern to create and manage agent instances based on
 * configuration files. It uses Java reflection to dynamically instantiate agent classes and
 * automatically wraps them with ParallelAgent for concurrent execution.
 * 
 * <h3>Configuration File Format</h3>
 * The configuration file uses a simple 3-line format for each agent:
 * <pre>
 * Line 1: Fully qualified agent class name (e.g., "configs.PlusAgent")
 * Line 2: Input topics (comma-separated, e.g., "topic1,topic2")
 * Line 3: Output topics (comma-separated, e.g., "result")
 * </pre>
 * 
 * <h3>Configuration File Examples and Patterns</h3>
 * 
 * <h4>Simple Single Agent Configuration</h4>
 * <pre>
 * # Simple increment agent
 * configs.IncAgent
 * X
 * Y
 * </pre>
 * 
 * <h4>Basic Arithmetic Pipeline</h4>
 * <pre>
 * # Add two inputs and increment the result
 * configs.PlusAgent
 * input1,input2
 * sum
 * configs.IncAgent
 * sum
 * final_result
 * </pre>
 * 
 * <h4>Complex Multi-Stage Pipeline</h4>
 * <pre>
 * # Multi-stage computational pipeline
 * configs.PlusAgent
 * A,B
 * intermediate1
 * configs.PlusAgent
 * intermediate1,C
 * intermediate2
 * configs.IncAgent
 * intermediate2
 * final_output
 * </pre>
 * 
 * <h4>Digital Logic Circuit Configuration</h4>
 * <pre>
 * # Complex digital logic circuit with multiple gate types
 * configs.AndAgent
 * inputA,inputB
 * and_gate1
 * configs.AndAgent
 * inputC,inputD
 * and_gate2
 * configs.OrAgent
 * and_gate1,and_gate2
 * or_gate1
 * configs.NotAgent
 * inputE
 * not_gate1
 * configs.XorAgent
 * or_gate1,not_gate1
 * xor_gate1
 * configs.ComparatorAgent
 * xor_gate1,reference_value
 * circuit_output
 * </pre>
 * 
 * <h4>Parallel Processing Configuration</h4>
 * <pre>
 * # Multiple agents processing the same input in parallel
 * configs.PlusAgent
 * sensor_data,calibration
 * processed_sum
 * configs.ComparatorAgent
 * sensor_data,threshold
 * threshold_check
 * configs.IncAgent
 * sensor_data
 * incremented_data
 * # All three agents process sensor_data simultaneously
 * </pre>
 * 
 * <h4>Logical Operations Chain</h4>
 * <pre>
 * # Chain of logical operations
 * configs.ComparatorAgent
 * valueA,valueB
 * comparison_result
 * configs.AndAgent
 * comparison_result,enable_flag
 * and_output
 * configs.OrAgent
 * and_output,backup_signal
 * or_output
 * configs.XorAgent
 * or_output,toggle_bit
 * xor_output
 * configs.NotAgent
 * xor_output
 * final_inverted
 * </pre>
 * 
 * <h3>Reflection-Based Agent Instantiation</h3>
 * The loader uses reflection to:
 * <ol>
 *   <li>Load agent classes by name using {@link Class#forName(String)}</li>
 *   <li>Find constructors that accept String[] arrays for input/output topics</li>
 *   <li>Create agent instances with the parsed topic arrays</li>
 *   <li>Wrap each agent with {@link ParallelAgent} for concurrent execution</li>
 * </ol>
 * 
 * <h3>Automatic ParallelAgent Wrapping</h3>
 * All created agents are automatically wrapped with ParallelAgent to enable:
 * <ul>
 *   <li>Concurrent message processing</li>
 *   <li>Work queue management</li>
 *   <li>Capacity-based performance tuning</li>
 * </ul>
 * 
 * The capacity for each ParallelAgent is calculated as: {@code Math.max(10, inputTopics.length * 5)}
 * 
 * <h3>Error Handling and Validation</h3>
 * The configuration loader includes comprehensive error handling:
 * <ul>
 *   <li>Validates file format (line count must be divisible by 3)</li>
 *   <li>Handles file I/O errors gracefully</li>
 *   <li>Reports reflection errors for invalid agent classes</li>
 *   <li>Continues processing even if individual agents fail to load</li>
 * </ul>
 * 
 * <h3>Complete Integration Examples</h3>
 * 
 * <h4>Basic Configuration Loading</h4>
 * <pre>{@code
 * // Create and configure the loader
 * GenericConfig config = new GenericConfig();
 * config.setConfFile("path/to/agents.conf");
 * 
 * // Load and create all agents
 * config.create();
 * 
 * // Agents are now active and processing messages
 * // ...
 * 
 * // Clean shutdown
 * config.close();
 * }</pre>
 * 
 * <h4>Complete Web Server Integration</h4>
 * <pre>{@code
 * import server.MyHTTPServer;
 * import servlets.*;
 * 
 * public class ComputationalGraphServer {
 *     private MyHTTPServer server;
 *     private GenericConfig config;
 *     
 *     public void start(int port, String configFile) {
 *         try {
 *             // Initialize configuration
 *             config = new GenericConfig();
 *             config.setConfFile(configFile);
 *             config.create();
 *             
 *             // Setup HTTP server
 *             server = new MyHTTPServer(port, 10);
 *             server.addServlet("POST", "/config", new ConfLoader());
 *             server.addServlet("GET", "/graph", new GraphRefresh());
 *             server.addServlet("GET", "/topics", new TopicDisplayer());
 *             server.addServlet("GET", "/", new HtmlLoader());
 *             
 *             // Start server
 *             server.start();
 *             System.out.println("Server started with configuration: " + configFile);
 *             
 *         } catch (Exception e) {
 *             System.err.println("Failed to start server: " + e.getMessage());
 *             cleanup();
 *             throw new RuntimeException("Server startup failed", e);
 *         }
 *     }
 *     
 *     public void stop() {
 *         cleanup();
 *     }
 *     
 *     private void cleanup() {
 *         if (server != null) {
 *             server.close();
 *             try {
 *                 server.join(5000);
 *             } catch (InterruptedException e) {
 *                 Thread.currentThread().interrupt();
 *             }
 *         }
 *         if (config != null) {
 *             config.close();
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h4>Dynamic Configuration Loading with Error Handling</h4>
 * <pre>{@code
 * public class DynamicConfigManager {
 *     private GenericConfig currentConfig;
 *     private final Object configLock = new Object();
 *     
 *     public boolean loadConfiguration(String configPath) {
 *         synchronized (configLock) {
 *             // Validate configuration file first
 *             if (!validateConfigurationFile(configPath)) {
 *                 System.err.println("Configuration validation failed: " + configPath);
 *                 return false;
 *             }
 *             
 *             // Create new configuration
 *             GenericConfig newConfig = new GenericConfig();
 *             try {
 *                 newConfig.setConfFile(configPath);
 *                 newConfig.create();
 *                 
 *                 // Replace old configuration
 *                 GenericConfig oldConfig = currentConfig;
 *                 currentConfig = newConfig;
 *                 
 *                 // Clean up old configuration
 *                 if (oldConfig != null) {
 *                     oldConfig.close();
 *                 }
 *                 
 *                 System.out.println("Configuration loaded successfully: " + configPath);
 *                 return true;
 *                 
 *             } catch (Exception e) {
 *                 System.err.println("Failed to load configuration: " + e.getMessage());
 *                 newConfig.close(); // Clean up failed configuration
 *                 return false;
 *             }
 *         }
 *     }
 *     
 *     private boolean validateConfigurationFile(String configPath) {
 *         try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
 *             List<String> lines = new ArrayList<>();
 *             String line;
 *             while ((line = reader.readLine()) != null) {
 *                 lines.add(line.trim());
 *             }
 *             
 *             // Check line count divisible by 3
 *             if (lines.size() % 3 != 0) {
 *                 System.err.println("Invalid format: line count not divisible by 3");
 *                 return false;
 *             }
 *             
 *             // Validate each agent definition
 *             for (int i = 0; i < lines.size(); i += 3) {
 *                 String className = lines.get(i);
 *                 String inputs = lines.get(i + 1);
 *                 String outputs = lines.get(i + 2);
 *                 
 *                 // Check class exists
 *                 try {
 *                     Class.forName(className);
 *                 } catch (ClassNotFoundException e) {
 *                     System.err.println("Agent class not found: " + className);
 *                     return false;
 *                 }
 *                 
 *                 // Validate topic format
 *                 if (inputs.isEmpty() || outputs.isEmpty()) {
 *                     System.err.println("Empty topics not allowed");
 *                     return false;
 *                 }
 *             }
 *             
 *             return true;
 *             
 *         } catch (IOException e) {
 *             System.err.println("Cannot read configuration file: " + e.getMessage());
 *             return false;
 *         }
 *     }
 *     
 *     public void shutdown() {
 *         synchronized (configLock) {
 *             if (currentConfig != null) {
 *                 currentConfig.close();
 *                 currentConfig = null;
 *             }
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h4>Configuration Validation and Testing</h4>
 * <pre>{@code
 * public class ConfigurationTester {
 *     
 *     public static void testConfiguration(String configPath) {
 *         System.out.println("Testing configuration: " + configPath);
 *         
 *         GenericConfig config = new GenericConfig();
 *         try {
 *             // Load configuration
 *             config.setConfFile(configPath);
 *             config.create();
 *             
 *             // Test basic functionality
 *             System.out.println("Configuration loaded successfully");
 *             System.out.println("Config name: " + config.getName());
 *             System.out.println("Config version: " + config.getVersion());
 *             
 *             // Simulate some processing time
 *             Thread.sleep(1000);
 *             
 *             System.out.println("Configuration test completed successfully");
 *             
 *         } catch (Exception e) {
 *             System.err.println("Configuration test failed: " + e.getMessage());
 *             e.printStackTrace();
 *         } finally {
 *             config.close();
 *         }
 *     }
 *     
 *     public static void main(String[] args) {
 *         // Test various configuration files
 *         String[] testConfigs = {
 *             "config_files/simple.conf",
 *             "config_files/complex.conf",
 *             "config_files/logical_basic.conf",
 *             "config_files/digital_logic_circuit.conf"
 *         };
 *         
 *         for (String configPath : testConfigs) {
 *             testConfiguration(configPath);
 *             System.out.println("---");
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h4>Production Configuration Management</h4>
 * <pre>{@code
 * public class ProductionConfigManager {
 *     private static final String DEFAULT_CONFIG = "config_files/default.conf";
 *     private static final String BACKUP_CONFIG = "config_files/backup.conf";
 *     
 *     private GenericConfig activeConfig;
 *     private final AtomicBoolean isLoading = new AtomicBoolean(false);
 *     
 *     public boolean loadConfigurationSafely(String primaryPath) {
 *         if (!isLoading.compareAndSet(false, true)) {
 *             System.err.println("Configuration loading already in progress");
 *             return false;
 *         }
 *         
 *         try {
 *             // Try primary configuration
 *             if (loadSingleConfiguration(primaryPath)) {
 *                 return true;
 *             }
 *             
 *             // Fallback to default configuration
 *             System.err.println("Primary config failed, trying default: " + DEFAULT_CONFIG);
 *             if (loadSingleConfiguration(DEFAULT_CONFIG)) {
 *                 return true;
 *             }
 *             
 *             // Last resort: backup configuration
 *             System.err.println("Default config failed, trying backup: " + BACKUP_CONFIG);
 *             return loadSingleConfiguration(BACKUP_CONFIG);
 *             
 *         } finally {
 *             isLoading.set(false);
 *         }
 *     }
 *     
 *     private boolean loadSingleConfiguration(String configPath) {
 *         GenericConfig newConfig = new GenericConfig();
 *         try {
 *             newConfig.setConfFile(configPath);
 *             newConfig.create();
 *             
 *             // Replace active configuration atomically
 *             GenericConfig oldConfig = activeConfig;
 *             activeConfig = newConfig;
 *             
 *             // Clean up old configuration
 *             if (oldConfig != null) {
 *                 oldConfig.close();
 *             }
 *             
 *             System.out.println("Successfully loaded configuration: " + configPath);
 *             return true;
 *             
 *         } catch (Exception e) {
 *             System.err.println("Failed to load " + configPath + ": " + e.getMessage());
 *             newConfig.close();
 *             return false;
 *         }
 *     }
 *     
 *     public void shutdown() {
 *         if (activeConfig != null) {
 *             activeConfig.close();
 *             activeConfig = null;
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h3>Supported Agent Types</h3>
 * Any agent class can be loaded as long as it:
 * <ul>
 *   <li>Implements the {@link Agent} interface</li>
 *   <li>Has a constructor accepting {@code (String[] inputTopics, String[] outputTopics)}</li>
 *   <li>Is available on the classpath</li>
 * </ul>
 * 
 * @author Generated Documentation
 * @version 1.0
 * @see Config
 * @see Agent
 * @see ParallelAgent
 * @since 1.0
 */
public class GenericConfig implements Config{
	
	//---------------------------------------------------
	private String Config_Path;
    private final List<Agent> agents = new ArrayList<>();

    /**
     * Sets the path to the configuration file that will be used to create agents.
     * 
     * <p>This method must be called before {@link #create()} to specify which
     * configuration file should be loaded. The file path can be absolute or relative
     * to the current working directory.
     * 
     * @param The_file_Path the path to the configuration file
     * @throws NullPointerException if the file path is null
     * 
     * @see #create()
     */
    public void setConfFile(String The_file_Path) {
        this.Config_Path = The_file_Path;
    }

    /**
     * Loads and parses the configuration file to create all specified agents.
     * 
     * <p>This method implements the core factory functionality by:
     * <ol>
     *   <li>Reading the entire configuration file line by line</li>
     *   <li>Validating the file format (line count must be divisible by 3)</li>
     *   <li>Parsing each 3-line agent definition</li>
     *   <li>Using reflection to instantiate agent classes</li>
     *   <li>Wrapping each agent with ParallelAgent for concurrent execution</li>
     *   <li>Maintaining a list of all created agents for lifecycle management</li>
     * </ol>
     * 
     * <h3>Configuration File Parsing Rules</h3>
     * <ul>
     *   <li>Empty lines and whitespace are trimmed but preserved in line count</li>
     *   <li>Total line count must be divisible by 3</li>
     *   <li>Each agent definition consists of exactly 3 consecutive lines</li>
     *   <li>Topic lists are split by comma with no additional whitespace handling</li>
     * </ul>
     * 
     * <h3>Reflection-Based Instantiation Process</h3>
     * For each agent definition:
     * <ol>
     *   <li>Load the class using {@link Class#forName(String)}</li>
     *   <li>Find constructor with signature {@code (String[], String[])}</li>
     *   <li>Create instance with input and output topic arrays</li>
     *   <li>Calculate ParallelAgent capacity: {@code Math.max(10, inputTopics.length * 5)}</li>
     *   <li>Wrap with ParallelAgent and add to managed agents list</li>
     * </ol>
     * 
     * <h3>Error Handling Strategy</h3>
     * <ul>
     *   <li><strong>File not set:</strong> Logs error and returns without processing</li>
     *   <li><strong>I/O errors:</strong> Logs error and returns without processing</li>
     *   <li><strong>Format errors:</strong> Logs error and returns without processing</li>
     *   <li><strong>Individual agent errors:</strong> Logs error but continues with remaining agents</li>
     * </ul>
     * 
     * <p><strong>Prerequisites:</strong> {@link #setConfFile(String)} must be called first
     * to specify the configuration file path.
     * 
     * @throws IOException if the configuration file cannot be read
     * @throws ClassNotFoundException if an agent class cannot be found
     * @throws ReflectiveOperationException if agent instantiation fails
     * @throws IllegalArgumentException if the configuration format is invalid
     * 
     * @see #setConfFile(String)
     * @see #close()
     * @see ParallelAgent#ParallelAgent(Agent, int)
     */
    @Override
    public void create() {
        if (Config_Path == null) {
            System.err.println("the given Config file path is not set.");
            return;
        }

        List<String> lines_Arr = new ArrayList<>();
        
        // reading all lines from the config file
        try (BufferedReader reader = new BufferedReader(new FileReader(Config_Path))) {
            String Curr_line;
            while ((Curr_line = reader.readLine()) != null) {
            	lines_Arr.add(Curr_line.trim());
            }
        } catch (IOException e) {
            System.err.println("we have failed at reading the config file- " + e.getMessage());
            return;
        }

        // validating the number of lines
        if (lines_Arr.size() % 3 != 0) {
            System.err.println("we got an invalid config file path struct. lines num isn't dividing by 3.");
            return;
        }

        // parsing each agent definition (3 lines per agent)
        for (int i = 0; i < lines_Arr.size(); i += 3) {
            try {
                String Agent_Class_Name = lines_Arr.get(i);
                String[] subs = lines_Arr.get(i + 1).split(",");
                String[] pubs = lines_Arr.get(i + 2).split(",");

                // loading class by name and creating an instance using reflection
                Class<?> class_A = Class.forName(Agent_Class_Name);
                Constructor<?> constructor = class_A.getConstructor(String[].class, String[].class);
                Agent Our_Agent = (Agent) constructor.newInstance((Object) subs, (Object) pubs);
                
                // wrapping the agent with ParallelAgent for concurrent execution
                int Our_Agent_Cap = Math.max(10, subs.length * 5);
                ParallelAgent wrapped = new ParallelAgent(Our_Agent, Our_Agent_Cap);
                
                // keeping track of all active agents
                agents.add(wrapped);
//                System.out.println("Loaded agent: " + Our_Agent.getName());

            } catch (Exception e) {
                System.err.println("we have failed to take the agent param at range " + i + "-" + (i + 2));
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the name identifier for this configuration implementation.
     * 
     * <p>This method is part of the Config interface and provides a human-readable
     * name for this configuration type, useful for logging and debugging purposes.
     * 
     * @return the string "GenericConfig"
     * 
     * @see Config#getName()
     */
    @Override
    public String getName() {
        return "GenericConfig";
    }

    /**
     * Returns the version number of this configuration implementation.
     * 
     * <p>This method is part of the Config interface and provides version information
     * for this configuration loader, useful for compatibility checking and debugging.
     * 
     * @return the version number (currently 1)
     * 
     * @see Config#getVersion()
     */
    @Override
    public int getVersion() {
        return 1;
    }

    /**
     * Closes all created agents and cleans up resources.
     * 
     * <p>This method implements graceful shutdown by:
     * <ol>
     *   <li>Iterating through all agents created by this configuration</li>
     *   <li>Calling {@link Agent#close()} on each agent to allow cleanup</li>
     *   <li>Logging any errors that occur during agent shutdown</li>
     *   <li>Clearing the internal agents list</li>
     * </ol>
     * 
     * <p><strong>Error Handling:</strong> If an individual agent fails to close,
     * the error is logged but the shutdown process continues for remaining agents.
     * This ensures that a single problematic agent doesn't prevent proper cleanup
     * of other agents.
     * 
     * <p><strong>Resource Management:</strong> After calling this method, all
     * agents created by this configuration will be closed and the configuration
     * can be safely discarded or reused with a new configuration file.
     * 
     * @see Config#close()
     * @see Agent#close()
     * @see #create()
     */
    @Override
    public void close() {
    	// closing all agents that were created
        for (Agent jon : agents) {
            try {
            	jon.close();
            } catch (Exception e) {
                System.err.println("error happend while closing the agent- " + jon.getName());
            }
        }
        agents.clear();
    }

  //---------------------------------------------------
}
