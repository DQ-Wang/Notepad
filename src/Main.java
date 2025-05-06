import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 加载FXML文件
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Notepad.fxml")); // 确保FXML路径正确
        Parent root = loader.load();

        // 创建场景
        Scene scene = new Scene(root, 1000, 800);

        // 加载CSS样式文件（确保 notepad.css 与 Notepad.fxml 在同一个目录下）
//        scene.getStylesheets().add(getClass().getResource("notepad.css").toExternalForm());

        // 设置窗口标题和场景
        primaryStage.setTitle("wdq的记事本");
        primaryStage.setScene(scene);
        primaryStage.show();


    }

    public static void main (String[]args){
        launch(args);
    }
}