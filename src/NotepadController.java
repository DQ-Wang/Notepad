import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.input.KeyEvent;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class NotepadController {
    // 与 FXML 文件中控件绑定的字段
    @FXML private TextArea textArea; // 主文本编辑区域
    @FXML private Label lineColumnLabel; // 状态栏中的行列信息标签
    @FXML private Label characterCountLabel; // 状态栏中的字符数量标签
    @FXML private Label zoomLabel; // 状态栏中的缩放比例标签
    @FXML private HBox statusBar; // 底部状态栏
    @FXML private MenuItem statusBarMenuItem; // “状态栏”菜单项
    @FXML private MenuItem deleteMenuItem;//在控制器中引入该组件，方便后续设置禁用
    @FXML private AnchorPane findPanel;
    @FXML private TextField findTextField;//查找写入框
    @FXML private AnchorPane updatePanel;
    @FXML private CheckMenuItem caseSensitiveMenuItem;//查找区分大小写
    @FXML private CheckMenuItem wrapCheckMenuItem;//查找是否回绕

    private int lastFindIndex=-1;

    // 当前缩放比例，初始为 1.0（即 100%）
    private double zoomFactor = 1.0;
    private final double ZOOM_STEP = 0.1; // 每次缩放的增量

    // 当前编辑的文件
    private File currentFile = null;
    private boolean isModified = false; // 文件是否被修改

    // 初始化方法，在 FXML 加载完毕后自动调用
    @FXML
    private void initialize() {
        // 设置按键和鼠标点击事件，用于更新状态栏内容
        textArea.setOnKeyReleased(this::updateStatus);
        textArea.setOnMouseClicked(e -> updateStatus(null));
        updateStatus(null); // 初始化时更新一次状态栏
        //监听选中内容的变化
        textArea.selectedTextProperty().addListener((obs,oldText,newText)->{
            deleteMenuItem.setDisable(newText.isEmpty());
        });
        //初始化时禁用“删除”菜单项
        deleteMenuItem.setDisable(true);
    }
    // 更新底部状态栏中的行列信息和字符数
    private void updateStatus(KeyEvent event) {
        String text = textArea.getText(); // 获取文本
        int caretPos = textArea.getCaretPosition(); // 当前光标位置

        // 计算当前行号
        int lineNum = text.substring(0, caretPos).split("\n", -1).length;

        // 计算当前列号
        int lastLineBreak = text.lastIndexOf("\n", Math.max(0, caretPos - 1));
        int columnNum = caretPos - lastLineBreak;

        // 设置标签显示
        lineColumnLabel.setText("行 " + lineNum + ", 列 " + columnNum);
        characterCountLabel.setText(text.length() + " 个字符");
    }

    @FXML private void newFile(ActionEvent event) {
        //如果文件被修改，提示用户保存
        if(isModified){
            Alert alert=new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("保存文件");
            alert.setHeaderText(null);
            alert.setContentText("文件已修改，是否保存？");
            ButtonType saveButton = new ButtonType("是", ButtonBar.ButtonData.YES);
            ButtonType dontSaveButton = new ButtonType("否", ButtonBar.ButtonData.NO);
            ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);
            Optional<ButtonType> result=alert.showAndWait();

            if(result.isPresent()) {
                if (result.get() == saveButton) {
                    saveFile();
                } else if (result.get() == cancelButton) {
                    return;

                }
            }


        }
        //清空文本区域和状态栏
        textArea.clear();
        currentFile=null;
        isModified=false;
        Stage stage=getStage(event);
        stage.setTitle("无标题-记事本");
        lineColumnLabel.setText("行 1, 列 1");
        characterCountLabel.setText("0 个字符");


    }

    @FXML private void newWindow() {
        try {
            //加载同一个FXML文件
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Notepad.fxml"));
            Parent root = loader.load();
            Stage newStage = new Stage();
            newStage.setTitle("无标题-记事本");
            newStage.setScene(new Scene(root,1000,800));
            newStage.show();
        }catch(IOException e){
            showAlert("无法创建新窗口");

        }

    }

    @FXML private void openFile(ActionEvent event) {//BUfferedReader和StringBuilder提高性能
        // 创建文件选择器
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("文本文件", "*.txt")//只允许选择txt文件
        );
//        Stage stage=(Stage)((Node)event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(getStage(event));
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder content = new StringBuilder();//创建StingBuilder对象，用于构建文件内容
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                }
                textArea.setText(content.toString());//将StringBuilder转换为String并设置到文本区域
                currentFile = file;
            } catch (IOException e) {
                showAlert("无法打开文件");
            }
        }
    }

    private Stage getStage() {
        return (Stage) textArea.getScene().getWindow();//获取当前窗口，传入到文件选择器中
    }

    private Stage getStage(Event event) {
        if (event.getSource() instanceof Node) {
            Node node = (Node) event.getSource();
            if (node.getScene() != null) {
                return (Stage) node.getScene().getWindow();
            }
        }
        return null;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void saveFile() {
        //如果当前文件为空，调用saveAsFile方法
            if(currentFile == null){
                saveAsFile();
                return;
            }//否则，将文本区域的内容写入到文件中
            try(BufferedWriter writer=new BufferedWriter(new FileWriter(currentFile))){
                writer.write(textArea.getText());
                isModified=false;
                getStage().setTitle(currentFile.getName()+"-记事本");
            }catch (IOException e){
                showAlert("无法保存文件");
            }


    }

    @FXML
    private void saveAsFile() {//类似于openFile方法
        FileChooser fileChooser=new FileChooser();
        fileChooser.setTitle("另存为");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("文本文件","*.txt")
        );
        File file =fileChooser.showSaveDialog(getStage());
        if(file!=null){
            try(BufferedWriter writer=new BufferedWriter(new FileWriter(file))){
                writer.write(textArea.getText());
                currentFile=file;
                isModified=false;
                getStage().setTitle(currentFile.getName()+"-记事本");
            }catch(IOException e){
                showAlert("无法保存文件");
            }
        }

    }

    @FXML
    private void exitApp() {
        Stage stage = (Stage) textArea.getScene().getWindow();
        stage.close();
    }
//undo-
    @FXML
    private void undo() {//就是Ctrl+Z
        textArea.undo();

    }

    @FXML
    private void redo() {
        textArea.redo();


    }

    @FXML
    private void cut() {
        textArea.cut();

    }

    @FXML
    private void copy() {
        textArea.copy();

    }

    @FXML
    private void paste() {
        textArea.paste();

    }

    @FXML
    private void selectAll() {
        textArea.selectAll();

    }


    @FXML
    private void delete() {
        String selectedText=textArea.getSelectedText();
        if(!selectedText.isEmpty()){
            //getSelection()方法返回TextRange对象，可以获取当前选取的起始和结束位置
            int start=textArea.getSelection().getStart();
            int end=textArea.getSelection().getEnd();
            textArea.deleteText(start,end);
        }

    }


    @FXML
    private void find() {
        findPanel.setVisible(true);
        findTextField.requestFocus();//自动获取输入焦点
        lastFindIndex=-1;//每次重新查找，重置索引


    }

    @FXML
    public void closeFindPanel() {
        findPanel.setVisible(false);
        updatePanel.setVisible(false);
    }
//查找不到用户提供的关键词时的响应办法
    private void showNotFoundAlert(String keyword) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "未找到 \"" + keyword + "\"。");
        alert.setHeaderText(null);
        alert.show();
    }


    @FXML
    private void findNext() {
        String content = textArea.getText();
        String keyword = findTextField.getText();
        if (keyword.isEmpty()) return;
        //判断是否勾选回绕或区分大小写
        boolean caseSensitive = caseSensitiveMenuItem.isSelected();
        boolean wrapEnabled = wrapCheckMenuItem.isSelected();
        //根据所得结果将字符串统一格式
        String searchContent = caseSensitive ? content : content.toLowerCase();
        String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        int start = textArea.getCaretPosition();
        int index = searchContent.indexOf(searchKeyword, start);

        if (index == -1 && wrapEnabled) {
            // 回绕：从头开始再查一次
            index = searchContent.indexOf(searchKeyword, 0);
        }

        if (index != -1) {
            //找到关键词，定位光标
            textArea.selectRange(index, index + keyword.length());
            textArea.requestFocus();
        } else {
            //找不到
            showNotFoundAlert(keyword);
        }

    }

    @FXML
    private void findPrevious() {
        String content = textArea.getText();
        String keyword = findTextField.getText();
        if (keyword.isEmpty()) return;

        boolean caseSensitive = caseSensitiveMenuItem.isSelected();
        boolean wrapEnabled = wrapCheckMenuItem.isSelected();

        String searchContent = caseSensitive ? content : content.toLowerCase();
        String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        int start = textArea.getCaretPosition() - keyword.length() - 1;
        if (start < 0) start = content.length();  // 位置靠前，查找从末尾

        int index = searchContent.lastIndexOf(searchKeyword, start);//从start处往前查找

        if (index == -1 && wrapEnabled) {
            // 回绕：从末尾重新查找
            index = searchContent.lastIndexOf(searchKeyword);
        }

        if (index != -1) {
            textArea.selectRange(index, index + keyword.length());
            textArea.requestFocus();
        } else {
            showNotFoundAlert(keyword);
        }
    }

    @FXML
    private void replace() {
        findPanel.setVisible(true);
        updatePanel.setVisible(true);

    }



    @FXML
    private void goTo() {

    }

    @FXML
    private void zoomIn() {

    }

    @FXML
    private void zoomOut() {

    }

    @FXML
    private void resetZoom() {

    }

    @FXML
    private void insertDateTime() {

    }

    @FXML
    private void toggleStatusBar() {

    }

    @FXML
    private void setFont() {

    }



}
