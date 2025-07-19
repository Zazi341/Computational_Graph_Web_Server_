# Computational Graph Web Server

A multi-threaded HTTP server implementation that provides a web interface for creating and managing computational graphs through configuration files. This project demonstrates advanced Java programming concepts including concurrent programming, design patterns, and web server architecture.

##  Table of Contents

- [Background](#background)
- [Features](#features)
- [Installation](#installation)
- [Running the Server](#running-the-server)
- [API Endpoints](#api-endpoints)
- [Configuration Files](#configuration-files)
- [Usage Examples](#usage-examples)
- [Skills Acquired](#skills-acquired)
- [Project Structure](#project-structure)
- [Documentation](#documentation)
- [Testing](#testing)
- [Contributing](#contributing)

##  Background

This project implements a computational graph system where:
- **Agents** are computational units that subscribe to topics and perform operations
- **Topics** act as message channels between agents using the publish-subscribe pattern
- **Configuration files** define the graph structure and agent connections
- **HTTP Server** provides REST endpoints for web interaction and visualization

The system supports various agent types including arithmetic operations (addition, increment), logical operations (AND, OR, NOT, XOR), and comparison operations, making it suitable for digital logic simulation, data processing pipelines, and educational demonstrations.

##  Features

- **Multi-threaded HTTP Server** with configurable thread pool
- **Servlet-based Request Routing** with longest-prefix matching
- **Real-time Graph Visualization** with interactive drag-and-drop interface
- **Dynamic Configuration Loading** from text files
- **Concurrent Agent Processing** with ParallelAgent wrapper
- **Topic Management System** with publish-subscribe pattern
- **Web-based Dashboard** for monitoring and interaction
- **Comprehensive API Documentation** with Javadoc
- **Thread-safe Operations** throughout the system

### Setup

1. **Clone or download the project:**
   ```bash
   git clone <https://github.com/Negbi/Computational_Graph_Web_Server/edit/main>
   cd computational-graph-server
   ```

2. **Compile the project:**
   ```bash
   javac -cp . *.java */*.java
   ```

3. **Verify compilation:**
   ```bash
   ls -la *.class */*.class
   ```

##  Running the Server


### Option 1: Manual compilation and execution
```bash
# Compile all Java files
javac -cp . *.java */*.java

# Run the main server
java Main
```

### Option 2: Background server (for API testing)
```bash
# Compile and run in background
javac MainServer.java
java MainServer &
```

### Server Startup
Once started, the server will display:
```
 Server started successfully on http://localhost:8080
 Open your browser and navigate to: http://localhost:8080/app/index.html
  Press <Enter> to stop the server...
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/app/index.html` | Main dashboard interface |
| `GET` | `/publish` | View current topics and their values |
| `GET` | `/graph` | Interactive graph visualization |
| `POST` | `/upload` | Upload configuration files |

### Example API Usage

```bash
# Check server status
curl http://localhost:8080/app/index.html

# View current topics
curl http://localhost:8080/publish

# Upload a configuration
curl -X POST http://localhost:8080/upload \
  -H "Content-Type: text/plain" \
  -d "configs.PlusAgent
input1,input2
output"

# View graph visualization
curl http://localhost:8080/graph
```

##  Configuration Files

Configuration files use a simple 3-line format for each agent:

```
Line 1: Agent class name (e.g., "configs.PlusAgent")
Line 2: Input topics (comma-separated)
Line 3: Output topics (comma-separated)
```

### Example Configurations

#### Simple Addition Chain
```
configs.PlusAgent
input1,input2
sum
configs.IncAgent
sum
result
```

#### Digital Logic Circuit
```
configs.AndAgent
inputA,inputB
and_gate1
configs.OrAgent
and_gate1,inputC
or_output
configs.NotAgent
or_output
final_output
```

### Available Agent Types

- **PlusAgent**: Addition of two inputs
- **IncAgent**: Increment by 1
- **AndAgent**: Bitwise AND operation
- **OrAgent**: Bitwise OR operation  
- **XorAgent**: Bitwise XOR operation
- **NotAgent**: Bitwise NOT operation
- **ComparatorAgent**: Three-way comparison (-1, 0, 1)

##  Usage Examples

### 1. Basic Server Setup
```java
// Create and start server
MyHTTPServer server = new MyHTTPServer(8080, 5);
server.addServlet("GET", "/api/status", new StatusServlet());
server.start();
```

### 2. Configuration Loading
```java
// Load computational graph from file
GenericConfig config = new GenericConfig();
config.setConfFile("config_files/simple.conf");
config.create();
```

### 3. Custom Agent Development
```java
public class CustomAgent implements Agent {
    public void callback(String topic, Message msg) {
        // Process message and publish result
        double result = processInput(msg.asDouble);
        TopicManagerSingleton.get().getTopic(outputTopic)
            .publish(new Message(result));
    }
}
```

##  Skills Acquired

During the development of this project, the following advanced programming skills were acquired:

### **Concurrent Programming**
- **Multi-threading**: Implemented thread-safe HTTP server with ExecutorService
- **Synchronization**: Used ConcurrentHashMap and volatile variables for thread safety
- **Producer-Consumer Pattern**: Implemented with ParallelAgent work queues
- **Deadlock Prevention**: Careful resource ordering and timeout mechanisms

### **Design Patterns**
- **Observer Pattern**: Topic-Agent subscription system
- **Factory Pattern**: GenericConfig for dynamic agent creation
- **Singleton Pattern**: TopicManagerSingleton for centralized management
- **Servlet Pattern**: HTTP request handling architecture
- **Strategy Pattern**: Different agent types with common interface

### **Web Development**
- **HTTP Protocol**: Manual implementation of HTTP/1.1 server
- **RESTful API Design**: Clean endpoint structure and HTTP methods
- **JSON Processing**: Dynamic data serialization for web interface
- **Real-time Visualization**: Interactive graph rendering with JavaScript
- **Template Processing**: Dynamic HTML generation with placeholder substitution

### **Software Architecture**
- **Modular Design**: Clear separation between server, graph, and configuration layers
- **Dependency Injection**: Loose coupling between components
- **Error Handling**: Comprehensive exception handling and graceful degradation
- **Resource Management**: Proper cleanup and lifecycle management
- **Documentation**: Comprehensive Javadoc with usage examples

### **Advanced Java Features**
- **Reflection**: Dynamic class loading and instantiation
- **Generics**: Type-safe collections and method signatures
- **Lambda Expressions**: Functional programming for event handling
- **Stream API**: Data processing and filtering operations
- **Package Organization**: Proper modularization and encapsulation

### **Testing and Debugging**
- **Unit Testing Strategies**: Comprehensive test coverage approaches
- **Integration Testing**: End-to-end system validation
- **Performance Monitoring**: Thread pool optimization and resource usage
- **Debugging Techniques**: Logging, tracing, and error diagnosis
- **API Testing**: Automated testing with curl and custom scripts

### **Development Tools and Practices**
- **Build Automation**: Compilation scripts and dependency management
- **Version Control**: Git workflow and project organization
- **Documentation Generation**: Javadoc automation and maintenance
- **Code Quality**: Consistent formatting and best practices
- **Deployment**: Production-ready configuration and startup scripts

##  Documentation

### Generate Javadoc
```bash
javadoc -d doc -cp . server/*.java configs/*.java graph/*.java servlets/*.java views/*.java
open doc/index.html
```

### Key Documentation Files
- **API Reference**: Complete Javadoc in `doc/` directory
- **Usage Examples**: Comprehensive code examples in class documentation
- **Configuration Guide**: Detailed examples in `config_files/`
- **Testing Guide**: API testing examples in `test_api.sh`

##  Testing

### Run API Tests
```bash
# Make test script executable
chmod +x test_api.sh

# Start server in background
java MainServer &

# Run comprehensive API tests
./test_api.sh
```

### Manual Testing
```bash
# Test configuration upload
curl -X POST http://localhost:8080/upload \
  --data-binary @config_files/simple.conf

# Test graph visualization
curl http://localhost:8080/graph

# Test topic status
curl http://localhost:8080/publish
```

### Web Interface Testing
1. Open http://localhost:8080/app/index.html
2. Upload configuration files through the web interface
3. View real-time graph visualization
4. Monitor topic values and agent status

## Contributing

This project demonstrates advanced Java programming concepts and serves as a foundation for:
- Educational computational graph systems
- Digital logic simulation platforms
- Real-time data processing pipelines
- Web-based monitoring dashboards

### Development Guidelines
- Follow existing code style and documentation standards
- Add comprehensive Javadoc for new classes and methods
- Include usage examples in documentation
- Ensure thread safety for concurrent operations
- Add appropriate error handling and logging

