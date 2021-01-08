package ie.gmit.sw;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.Scanner;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.stage.*;
import javafx.scene.layout.*;
import javafx.util.*;

import one.microstream.storage.types.EmbeddedStorage;
import one.microstream.storage.types.EmbeddedStorageManager;

/**
 * The App Window Class. The app window class handles all transactions of
 * the provided jar file. It uses microstream's embeddedstorage manager to
 * store the retreived data into a data folder.
 * Note: files inside the data folder should be manually deleted whenever
 * this program is run
 *
 *  @author Nathan Garrihy
 *  @version 1.0 Beta
 *  @since 2021-01-08
 */

public class AppWindow extends Application {
    private List<Class> root = new ArrayList<>();
    private EmbeddedStorageManager db = null; // Microstream's embeddedstoragemanager = the database

    private ObservableList<Jrec> jrecs; // The Model (a list of observers)
    private TableView<Jrec> tv; // The View (a composite of GUI components)
    private TextField txtFile; // A control (part of the View and a leaf node)

    @Override
    // Template Method
    public void start(Stage stage) throws Exception {
        // Set up the model
        jrecs = InitializeJrecs();
        db = EmbeddedStorage.start(root, Paths.get("./data"));

        /*
         * The GUI is based on the ** Composite Pattern ** and is a tree of nodes, some
         * of which are composite nodes (containers) and some are leaf nodes (controls).
         * A stage contains 1..n scenes, each of which is a container window for other
         * containers or controls.
         *
         * JavaFX, Android and most GUI frameworks allow the creation of windows using
         * a declarative format, typically XML. In the case of JavaFX, the syntax is
         * called FXML. The idea of this (which is quite an old one!), is to separate
         * the View from the Controller and Model (good practice) and allow non-programmers
         * to create Views in XML using SceneBuilder or the equivalent that should integrate
         * seamlessly with a suite of controllers and models designed by a programmer. In
         * practice, I find that XML slows down development to a crawl and I prefer to
         * programme the GUI from scratch, as it's much quicker, even if it is verbose.
         */
        stage.setTitle("Nathan Garrihy - G00354922 - GMIT - B.Sc. in Computing (Software Development)");
        stage.setWidth(800);
        stage.setHeight(600);

        /* The following is an example of the ** Observer Pattern**. Use a lambda
         * expression to plant an EventHandler<WindowEvent> observer on the stage
         * close button. The "click" will be queued and handled by an event dispatch
         * thread when it gets a chance.
         */
        stage.setOnCloseRequest((e) -> System.exit(0));

        /*
         * Create the MVC View using the **Composite Pattern**. We can Build the GUi
         * tree using one or more composites to create branches and one or more controls
         * to handle user interactions. Composites and containers and controls are leaf
         * nodes that the user can send gestures to.
         *
         * The root container we will use is a VBox. All the subtypes of the class Pane
         * are composite nodes and can be used as containers for other nodes (composites
         * and leaf nodes). The choice of container is also an example of the **Strategy
         * Pattern**. The Scene object is the Context and the layout container (AnchorPane,
         * BorderPanel, VBox, FlowPane etc) is a concrete strategy.
         */

        VBox box = new VBox();
        box.setPadding(new Insets(10));
        box.setSpacing(8);

        //**Strategy Pattern**. Configure the Context with a Concrete Strategy
        Scene scene = new Scene(box);
        stage.setScene(scene);

        ToolBar toolBar = new ToolBar(); //A ToolBar is a composite node for Buttons (leaf nodes)

        Button btnQuit = new Button("Quit"); //A Leaf node
        btnQuit.setOnAction(e -> System.exit(0)); //Plant an observer on the button
        toolBar.getItems().add(btnQuit); //Add to the parent node and build the tree

        Button btnAdd = new Button("Save"); //A Leaf node
        btnAdd.setOnAction(e -> { //Plant an observer on the button
            /*
             * For the sake of simplicity of explanation, the instance of Jrec is
             * hard-wired into the application. The important things to note are the
             * following:
             *
             *   1) This lambda implementation of EventHandler<ActionEvent> is an
             *      example of a **Controller** in the MVC.
             *   2) We update the View (TableView) by changing the Model (ObservableList),
             *      i.e. we never directly update the view.
             *
             */
            query();
        });
        toolBar.getItems().add(btnAdd); //Add to the parent node and build the tree

        Button btnDelete = new Button("Delete"); //A Leaf node
        btnDelete.setOnAction(e -> { //Plant an observer on the button
            /*
             * Get the selected Jar Record object from the View (TableView) and
             * remove it from the Model (ObservableList). Do not try to update
             * the view directly.
             */
            Jrec c = tv.getSelectionModel().getSelectedItem();
            jrecs.remove(c);
        });
        toolBar.getItems().add(btnDelete); //Add to the parent node and build the tree
        /*
         * Add all the sub trees of nodes to the parent node and build the tree
         */
        box.getChildren().add(getFileChooserPane(stage)); //Add the sub tree to the main tree
        box.getChildren().add(getTableView()); //Add the sub tree to the main tree
        box.getChildren().add(toolBar); //Add the sub tree to the main tree
        // Display the window
        stage.show();
        stage.centerOnScreen();
    }

    /**
     *
     * This method builds a TitledPane containing the controls for the file chooser
     * part of the application. We could have created a specialised instance of the
     * class TitledPane using inheritance and moved all of the method into its own
     * class (OCP). Gets the file chooser pane.

     * @param stage  the stage
     * @return the file chooser pane
     * @throws NoClassDefFoundError
     */
    private TitledPane getFileChooserPane(Stage stage) throws NoClassDefFoundError, ClassNotFoundException {
        VBox panel = new VBox(); //** A concrete strategy ***

        txtFile = new TextField(); //A leaf node

        FileChooser fc = new FileChooser(); //A leaf node
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));

        Button btnOpen = new Button("Select File"); //A leaf node
        btnOpen.setOnAction(e -> { //Plant an observer on the button
            File f = fc.showOpenDialog(stage);
            txtFile.setText(f.getAbsolutePath());
        });

        Button btnProcess = new Button("Process"); //A leaf node
        btnProcess.setOnAction(e -> { //Plant an observer on the button
            File f = new File(txtFile.getText());
            System.out.println("[INFO] Processing file " + f.getName());

            JarInputStream in = null;
            try {
                in = new JarInputStream(new FileInputStream(new File(f.toString())));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            JarEntry next = null;
            try {
                next = in.getNextJarEntry();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            while (next != null) {
                if (next.getName().endsWith(".class")) {
                    String name = next.getName().replaceAll("/", "\\.");
                    name = name.replaceAll(".class", "");
                    if (!name.contains("$")) name.substring(0, name.length() - ".class".length());

                    Class cls;
                    try {
                        cls = Class.forName(name);
                        root.add(cls);

                        String classname = cls.getName();
                        String packageName = cls.getPackageName().toString();
                        boolean isInteface = cls.isInterface();
                        int SLOC = getSloc(in);

                        jrecs.add(new Jrec(classname, packageName, isInteface, SLOC));

                    } catch (ClassNotFoundException classNotFoundException) {
                        classNotFoundException.printStackTrace();
                    }catch (NoClassDefFoundError noClassDefFoundError)
                    {
                        //catch the no class defer error
                    } catch (FileNotFoundException fileNotFoundException) {
                        fileNotFoundException.printStackTrace();
                    }
                    storeData();
                    System.out.println(name);
                }
                try {
                    next = in.getNextJarEntry();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        ToolBar tb = new ToolBar(); //A composite node
        tb.getItems().add(btnOpen); //Add to the parent node and build a sub tree
        tb.getItems().add(btnProcess); //Add to the parent node and build a sub tree

        panel.getChildren().add(txtFile); //Add to the parent node and build a sub tree
        panel.getChildren().add(tb); //Add to the parent node and build a sub tree

        TitledPane tp = new TitledPane("Select File to Process", panel); //Add to the parent node and build a sub tree
        tp.setCollapsible(false);
        return tp;
    }

    /**
     *
     * Iterates through the code counting and returning the
     * total number of lines of source code
     *
     * @param jarInputStream the jarInputStream
     * @return the s.l.o.c.
     * @throws FileNotFoundException
     */
    private int getSloc(JarInputStream jarInputStream) throws FileNotFoundException{
        int count = 0;
        try {
            // create a new file object
            // create an object of Scanner
            // associated with the file
            Scanner sc = new Scanner(jarInputStream);
            // read each line and
            // count number of lines
            while(sc.hasNextLine()) {
                sc.nextLine();
                count++;
            }
        } catch (Exception e) {
            e.getStackTrace();
        }
        return  count;
    }

    /*
     * This method builds a table to display the jrec details. This View
     * could also have been encapsulated inside its own class using the signature
     *
     * public class JrecTableView extends TableView<Jrec>
     *
     */

    /**
     *
     * Initialize jar records
     *
     * @return ObservableList<Jrec> (list from jar file)
     */
    private ObservableList<Jrec> InitializeJrecs(){
        jrecs = FXCollections.observableArrayList();
        return jrecs;
    }

    /**
     * Store data() method
     *
     * stores data to microstream's embeddedstoragemanager database
     */
    void storeData() {
        try {
            db.storeRoot();
            db.shutdown();
        }    catch (NoClassDefFoundError | Exception e) {
            System.out.println("Exception in storeData()");
        }
    }

    /**
     * Query is used to iterate over the list of classes,
     * returning a sequential Stream with this list as its source.
     */
    void query(){
        root.stream().forEach(System.out::println);;
    }

    /**
     *
     * Gets the table view
     *
     * @return the table view
     */
    private TableView<Jrec> getTableView() {

        //The next line is **very important**. We configure a View (TableView) with
        //a Model (ObservableList<Jrec>).
        tv = new TableView<>(jrecs); //A TableView is a composite node
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); //Stretch columns to fit the window

        /*
         *  Create a TableColumn from the class Jrec that displays some attribute
         *  as a String. This field is Observable and the method call() will be fired
         *  when the table is being refreshed or when the model is updated. The instance
         *  of the interface Callback is implemented as an anonymous inner class and acts
         *  as a Controller for the table column. Callback is also an example of an Observer
         *  and the inner class a ConcreteObserver. The method call() is analogous to the
         *  notify() method in the Observer Pattern.
         */

        /**
         *
         * Call for the name of the class
         *
         * @param the column class name
         * @param p the String
         * @return ObservableValue<String>
         */
        TableColumn<Jrec, String> className = new TableColumn<>("Class Name");
        className.setCellValueFactory(new Callback<CellDataFeatures<Jrec, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(CellDataFeatures<Jrec, String> p) {
                return new SimpleStringProperty(p.getValue().className());
            }
        });

        /**
         *
         * Call for the name of the package
         *
         * @param the column package name
         * @param p the String
         * @return ObservableValue<String>
         */
        TableColumn<Jrec, String> packageName = new TableColumn<>("Package Name");
        packageName.setCellValueFactory(new Callback<CellDataFeatures<Jrec, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(CellDataFeatures<Jrec, String> p) {
                return new SimpleStringProperty(p.getValue().packageName().toString());
            }
        });

        /**
         *
         * Call for boolean telling whether the file is an interface or not
         *
         * @param the column isInterface
         * @param p the boolean
         * @return ObservableValue<Boolean>
         */
        TableColumn<Jrec, Boolean> isInterface = new TableColumn<>("isInterface");
        isInterface.setCellValueFactory(new Callback<CellDataFeatures<Jrec, Boolean>, ObservableValue<Boolean>>() {
            public ObservableValue<Boolean> call(CellDataFeatures<Jrec, Boolean> p) {
                return new SimpleBooleanProperty(p.getValue().isInterface());
            }
        });

        /**
         *
         * Call for the total number of Source Lines of Code in the file
         *
         * @param the column sloc
         * @param p the number
         * @return ObservableValue<Number>
         */
        TableColumn<Jrec, Number> sloc = new TableColumn<>("S.L.O.C");
        sloc.setCellValueFactory(new Callback<CellDataFeatures<Jrec, Number>, ObservableValue<Number>>() {
            public ObservableValue<Number> call(CellDataFeatures<Jrec, Number> p) {
                return new SimpleIntegerProperty(p.getValue().sloc());
            }
        });

        tv.getColumns().add(className); //Add class name to the tree
        tv.getColumns().add(packageName);  //Add package name to the tree
        tv.getColumns().add(isInterface); // Add interface boolean of the tree
        tv.getColumns().add(sloc); // Add source lines of code to the tree
        return tv;
    }
}