package ie.gmit.sw;

import javafx.application.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import one.microstream.storage.types.EmbeddedStorage;
import one.microstream.storage.types.EmbeddedStorageManager;

public class AppWindow extends Application {
    private List<Class> root = new ArrayList<>();
    private EmbeddedStorageManager db = null; // The storage manager is the database...

    private ObservableList<JarRecord> customers; //The Model - a list of observers.
    private TableView<JarRecord> tv; //The View - a composite of GUI components
    private TextField txtFile; //A control, part of the View and a leaf node.

    @Override
    public void start(Stage stage) throws Exception { //This is a ***Template Method***
        customers = InitializeCustomers(); //Get the Model
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
        stage.setTitle("GMIT - B.Sc. in Computing (Software Development)");
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

        Button btnAdd = new Button("Add"); //A Leaf node
        btnAdd.setOnAction(e -> { //Plant an observer on the button
            /*
             * For the sake of simplicity of explanation, the instance of Customer is
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
             * Get the selected Customer object from the View (TableView) and
             * remove it from the Model (ObservableList). Do not try to update
             * the view directly.
             */

            JarRecord c = tv.getSelectionModel().getSelectedItem();
            customers.remove(c);
        });
        toolBar.getItems().add(btnDelete); //Add to the parent node and build the tree

        /*
         * Add all the sub trees of nodes to the parent node and build the tree
         */
        box.getChildren().add(getFileChooserPane(stage)); //Add the sub tree to the main tree
        box.getChildren().add(getTableView()); //Add the sub tree to the main tree
        box.getChildren().add(toolBar); //Add the sub tree to the main tree
        //box.getChildren().add(new PolyPanel());
        // Display the window
        stage.show();
        stage.centerOnScreen();
    }

    void storeData() {
        try {
            db.storeRoot();
            db.shutdown();
        }	catch (NoClassDefFoundError | Exception e) {
            // Exception caught
        }
    }

    private ObservableList<JarRecord> InitializeCustomers(){
        customers = FXCollections.observableArrayList();
        return customers;
    }

    /*
     *  This method builds a TitledPane containing the controls for the file chooser
     *  part of the application. We could have created a specialised instance of the
     *  class TitledPane using inheritance and moved all of the method into its own
     *  class (OCP).
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
						String className = cls.getName();
						String packageName = cls.getPackageName();
						boolean isInterface = cls.isInterface();
						int sloc = getSLOC(in);

						root.add(cls);
                        db.database();

						JarRecord classRecord = new JarRecord(className, packageName, isInterface, sloc);

                        customers.add(
                                new JarRecord(className, packageName, isInterface, sloc)
                        );

					} catch (ClassNotFoundException classNotFoundException) {
						classNotFoundException.printStackTrace();
					} catch (NoClassDefFoundError noClassDefFoundError)
					{
					    noClassDefFoundError.printStackTrace();
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

    private void query(){
        root.stream().forEach(System.out::println);;
    }

    private int getSLOC(JarInputStream jis) throws FileNotFoundException {
        int count = 0;

        try {
            // create an object of Scanner
            // associated with the file
            Scanner sc = new Scanner(jis);

            // read each line and
            // count number of lines
            while (sc.hasNextLine()) {
                sc.nextLine();
                count++;
            }

        } catch (Exception e) {
            e.getStackTrace();
        }
        return count;
    }

    /*
     * This method builds a table to display the customer details. This View
     * could also have been encapsulated inside its own class using the signature
     *
     * public class CustomerTableView extends TableView<Customer>
     *
     */
    private TableView<JarRecord> getTableView() {
        /*
         * The next line is **very important**. We configure a View (TableView) with
         * a Model (ObservableList<Customer>). The Model is observable and will
         * propagate any changes to it to the View or Views that render it.
         */
        tv = new TableView<>(customers); //A TableView is a composite node
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); //Stretch columns to fit the window

        /*
         *  Create a TableColumn from the class Customer that displays some attribute
         *  as a String. This field is Observable and the method call() will be fired
         *  when the table is being refreshed or when the model is updated. The instance
         *  of the interface Callback is implemented as an anonymous inner class and acts
         *  as a Controller for the table column. Callback is also an example of an Observer
         *  and the inner class a ConcreteObserver. The method call() is analogous to the
         *  notify() method in the Observer Pattern.
         */

        //Creates an observable table column from a String field extracted from the Customer class
        TableColumn<JarRecord, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(new Callback<CellDataFeatures<JarRecord, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(CellDataFeatures<JarRecord, String> p) {
                return new SimpleStringProperty(p.getValue().name());
            }
        });

        //Creates an observable table column from a String field extracted from the Customer class
		TableColumn<JarRecord, String> pack = new TableColumn<>("Package");
		pack.setCellValueFactory(new Callback<CellDataFeatures<JarRecord, String>, ObservableValue<String>>() {
			public ObservableValue<String> call(CellDataFeatures<JarRecord, String> p) {
				return new SimpleStringProperty(p.getValue().pack().toString());
			}
		});

        //Creates an observable table column from an Image attribute of the Customer class
		TableColumn<JarRecord, Boolean> isInterface = new TableColumn<>("Interface");
		isInterface.setCellValueFactory(new Callback<CellDataFeatures<JarRecord, Boolean>, ObservableValue<Boolean>>() {
			public ObservableValue<Boolean> call(CellDataFeatures<JarRecord, Boolean> p) {
				return new SimpleBooleanProperty(p.getValue().bool());
			}
		});

        TableColumn<JarRecord, Number> sloc = new TableColumn<>("S.L.O.C");
        sloc.setCellValueFactory(new Callback<CellDataFeatures<JarRecord, Number>, ObservableValue<Number>>() {
            public ObservableValue<Number> call(CellDataFeatures<JarRecord, Number> p) {
                return new SimpleIntegerProperty(p.getValue().sloc());
            }
        });

        tv.getColumns().add(name); //Add name to the tree
        tv.getColumns().add(pack); //Add pack to the tree
        tv.getColumns().add(isInterface);  //Add bool to the tree
        tv.getColumns().add(sloc);  //Add source lines of code to the tree
        return tv;
    }
}