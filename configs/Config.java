package configs;

/**
 * Config defines the contract for configuration management systems that create and manage
 * computational graph agents from configuration files.
 * 
 * <p>The Config interface implements the factory pattern for agent creation, where configuration
 * files define the structure of computational graphs including agent types, input topics,
 * and output topics. Implementations parse these configuration files and instantiate the
 * appropriate agent objects using reflection.
 * 
 * <h3>Configuration Lifecycle</h3>
 * <p>The typical configuration lifecycle follows this pattern:
 * <ol>
 * <li><strong>Setup:</strong> Configuration file path is set (implementation-specific)</li>
 * <li><strong>Creation:</strong> {@link #create()} parses the file and instantiates agents</li>
 * <li><strong>Operation:</strong> Agents process messages according to the graph structure</li>
 * <li><strong>Cleanup:</strong> {@link #close()} releases all created agents and resources</li>
 * </ol>
 * 
 * <h3>Configuration File Format</h3>
 * <p>The standard configuration format uses a 3-line pattern per agent:
 * <pre>
 * [Agent Class Name]     # Fully qualified class name (e.g., configs.PlusAgent)
 * [Input Topics]         # Comma-separated list of input topic names
 * [Output Topics]        # Comma-separated list of output topic names
 * </pre>
 * 
 * <h3>Agent Factory Pattern</h3>
 * <p>Implementations use reflection to dynamically instantiate agent classes:
 * <ul>
 * <li>Load agent class by name using {@code Class.forName()}</li>
 * <li>Find constructor that accepts {@code String[], String[]} parameters</li>
 * <li>Create agent instance with input and output topic arrays</li>
 * <li>Optionally wrap agents with concurrency wrappers (e.g., ParallelAgent)</li>
 * </ul>
 * 
 * <h3>Versioning and Compatibility</h3>
 * <p>The {@link #getVersion()} method supports configuration versioning for:
 * <ul>
 * <li>Backward compatibility with older configuration formats</li>
 * <li>Migration support when configuration schemas change</li>
 * <li>Runtime validation of configuration compatibility</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Create and configure the config manager
 * GenericConfig config = new GenericConfig();
 * config.setConfFile("path/to/agents.conf");
 * 
 * try {
 *     // Parse configuration and create agents
 *     config.create();
 *     
 *     // Configuration is now active, agents are processing messages
 *     System.out.println("Configuration: " + config.getName());
 *     System.out.println("Version: " + config.getVersion());
 *     
 *     // ... application runs ...
 *     
 * } finally {
 *     // Clean up all created agents
 *     config.close();
 * }
 * }</pre>
 * 
 * <h3>Error Handling</h3>
 * <p>Implementations should handle various error conditions gracefully:
 * <ul>
 * <li>Missing or unreadable configuration files</li>
 * <li>Invalid configuration file format</li>
 * <li>Unknown agent class names</li>
 * <li>Agent instantiation failures</li>
 * <li>Resource cleanup errors during close</li>
 * </ul>
 * 
 * @see configs.GenericConfig
 * @see graph.Agent
 * @see graph.ParallelAgent
 * @since 1.0
 */
public interface Config {
    
    /**
     * Parses the configuration file and creates all specified agents in the computational graph.
     * 
     * <p>This method performs the core factory functionality by:
     * <ol>
     * <li>Reading and parsing the configuration file</li>
     * <li>Validating the configuration format and structure</li>
     * <li>Using reflection to instantiate agent classes</li>
     * <li>Registering agents with the topic management system</li>
     * <li>Optionally wrapping agents with concurrency or monitoring wrappers</li>
     * </ol>
     * 
     * <p>The method should be idempotent - calling it multiple times should not create
     * duplicate agents or cause errors. Subsequent calls may either be ignored or
     * may replace the existing configuration.
     * 
     * <h3>Configuration File Parsing</h3>
     * <p>The standard parsing process:
     * <ul>
     * <li>Read all lines from the configuration file</li>
     * <li>Validate that the number of lines is divisible by 3</li>
     * <li>Process each 3-line group as an agent definition</li>
     * <li>Handle empty lines and comments appropriately</li>
     * </ul>
     * 
     * <h3>Agent Instantiation</h3>
     * <p>For each agent definition:
     * <pre>{@code
     * // Load the agent class
     * Class<?> agentClass = Class.forName(className);
     * 
     * // Find the constructor
     * Constructor<?> constructor = agentClass.getConstructor(String[].class, String[].class);
     * 
     * // Create the agent instance
     * Agent agent = (Agent) constructor.newInstance(inputTopics, outputTopics);
     * 
     * // Optionally wrap with ParallelAgent for concurrency
     * ParallelAgent wrapped = new ParallelAgent(agent, capacity);
     * }</pre>
     * 
     * <h3>Error Handling</h3>
     * <p>The method should handle errors gracefully:
     * <ul>
     * <li>Log specific error messages for debugging</li>
     * <li>Continue processing other agents if one fails</li>
     * <li>Provide meaningful error context (line numbers, agent names)</li>
     * <li>Clean up partially created resources on failure</li>
     * </ul>
     * 
     * @throws RuntimeException if the configuration file cannot be read or parsed
     * @throws ClassNotFoundException if an agent class cannot be found
     * @throws ReflectiveOperationException if agent instantiation fails
     * @throws IllegalArgumentException if the configuration format is invalid
     * 
     * @see #close()
     * @see graph.Agent
     */
    void create();
    
    /**
     * Returns the human-readable name of this configuration implementation.
     * 
     * <p>The name should identify the configuration type or implementation strategy.
     * Common names include "GenericConfig", "XMLConfig", "JSONConfig", etc.
     * 
     * <p>This name is used for:
     * <ul>
     * <li>Logging and debugging output</li>
     * <li>Configuration type identification</li>
     * <li>Error reporting and diagnostics</li>
     * <li>Administrative interfaces</li>
     * </ul>
     * 
     * @return the configuration name, never null or empty
     */
    String getName();
    
    /**
     * Returns the version number of this configuration implementation.
     * 
     * <p>The version number supports:
     * <ul>
     * <li><strong>Backward Compatibility:</strong> Older configuration files can be detected and handled appropriately</li>
     * <li><strong>Feature Detection:</strong> Applications can check for specific configuration features</li>
     * <li><strong>Migration Support:</strong> Configuration upgrades can be managed systematically</li>
     * <li><strong>Debugging:</strong> Version information helps diagnose compatibility issues</li>
     * </ul>
     * 
     * <p>Version numbering should follow a consistent scheme:
     * <ul>
     * <li>Increment for backward-compatible changes</li>
     * <li>Major version changes for breaking changes</li>
     * <li>Use semantic versioning principles where appropriate</li>
     * </ul>
     * 
     * <h3>Version Compatibility Example</h3>
     * <pre>{@code
     * Config config = new GenericConfig();
     * if (config.getVersion() >= 2) {
     *     // Use newer features
     *     config.enableAdvancedFeatures();
     * } else {
     *     // Fall back to basic functionality
     *     config.useBasicMode();
     * }
     * }</pre>
     * 
     * @return the configuration version number, typically starting from 1
     */
    int getVersion();
    
    /**
     * Releases all resources and cleans up all agents created by this configuration.
     * 
     * <p>This method performs comprehensive cleanup:
     * <ol>
     * <li>Call {@link graph.Agent#close()} on all created agents</li>
     * <li>Clear internal agent collections and references</li>
     * <li>Release any file handles or external resources</li>
     * <li>Reset internal state to allow for reconfiguration</li>
     * </ol>
     * 
     * <p>The method should be idempotent - calling it multiple times should not
     * cause errors or unexpected behavior. It should also be safe to call even
     * if {@link #create()} was never called or failed.
     * 
     * <h3>Agent Cleanup Process</h3>
     * <p>For each created agent:
     * <pre>{@code
     * for (Agent agent : createdAgents) {
     *     try {
     *         agent.close();
     *     } catch (Exception e) {
     *         // Log error but continue cleanup
     *         System.err.println("Failed to close agent: " + agent.getName());
     *         e.printStackTrace();
     *     }
     * }
     * createdAgents.clear();
     * }</pre>
     * 
     * <h3>Resource Management</h3>
     * <p>Proper resource management includes:
     * <ul>
     * <li>Closing file handles and streams</li>
     * <li>Shutting down thread pools and executors</li>
     * <li>Releasing network connections</li>
     * <li>Clearing topic subscriptions and publications</li>
     * </ul>
     * 
     * <p>This method is typically called:
     * <ul>
     * <li>During application shutdown</li>
     * <li>When reconfiguring the system</li>
     * <li>In finally blocks for resource cleanup</li>
     * <li>During error recovery procedures</li>
     * </ul>
     * 
     * @see #create()
     * @see graph.Agent#close()
     */
    void close();
}
