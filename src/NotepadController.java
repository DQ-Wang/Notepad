import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.input.KeyEvent;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    @FXML private TextField updateTextField;//替换写入框
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

    /**
     *
     * @param message 要输出的信息
     */
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

            // 更新状态栏
            updateStatus(null);
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

            // 更新状态栏
            updateStatus(null);
        } else {
            showNotFoundAlert(keyword);
        }
    }

    @FXML
    private void replace() {
        findPanel.setVisible(true);
        updatePanel.setVisible(true);
        //读取查找&替换的写入
        String keyword=findTextField.getText();
        String replacement=updateTextField.getText();
        if(keyword.isEmpty()) return ;

        String selectedText=textArea.getSelectedText();//获取当前光标选中的内容
        boolean caseSensitive=caseSensitiveMenuItem.isSelected();
        //判断当前选中的内容是否是要替换的目标
        boolean match=false;
        if(caseSensitive){//大小写敏感
            match=keyword.equals(selectedText);//只有两个字符串完全相等时才能返回true
        }                                  //若一个是Apple一个是apple都不行
        else{
            match=keyword.equalsIgnoreCase(selectedText);
        }

        //如果当前所选中的内容正好是要替换的内容，在进行替换
        if(match){
            int start =textArea.getSelection().getStart();
            int end=textArea.getSelection().getEnd();
            textArea.replaceText(start,end,replacement);
        }

        //自动查找下一个匹配项
        findNext();


    }

    @FXML
    private void replaceAll(){
        //内容捕获
        String keyword =findTextField.getText();
        String replacement=updateTextField.getText();
        if(keyword.isEmpty()) return ;

        String content=textArea.getText();
        boolean caseSensitive=caseSensitiveMenuItem.isSelected();


        int count=0;//记录替换次数
        if(!caseSensitive){
            //不区分大小写，用正则替换
            //Pattern.qupte(keyword)把关键词当作“普通字符”处理，而不是正则语法。
            //keyword 是 "a.c"，正则会认为这是：a 开头，中间任意字符，最后是 c
            //如果我们只是想找 "a.c" 这个字面字符串，而不是正则的意思，就得加上 Pattern.quote(...)
            //Pattern.CASE_INSENSITIVE 表示不要区分大小写
            //Pattern.compile()方法将
            Pattern pattern=Pattern.compile(Pattern.quote(keyword),Pattern.CASE_INSENSITIVE);
            Matcher matcher=pattern.matcher(content);
            StringBuffer sb=new StringBuffer();

            while(matcher.find()){
                //matcher.appendReplacement:它会把当前匹配到的内容替换成你给的新内容，并且把之前没匹配的内容 + 替换后的内容追加到 sb 里。
                matcher.appendReplacement(sb,Matcher.quoteReplacement(replacement));
                count++;
            }
            matcher.appendTail(sb);

            if(count>0){
                textArea.setText(sb.toString());
            }

        }
        else {
            //区分大小写，就用简单的替换法
            int index=0;
            StringBuilder sb=new StringBuilder();
            while(true){
                int next=content.indexOf(keyword,index);
                if(next==-1) break;
                //把上次匹配之后到这次匹配之前的内容（即未被匹配的部分）加进 sb
                //把 keyword 替换成 replacement，加进 sb。
                sb.append(content,index,next).append(replacement);
                //更新 index，准备从刚刚替换掉的词后面继续查找下一个匹配。
                index =next+keyword.length();
                count++;
            }
            //循环结束后，把最后一段没被匹配的文字也加到结果里。
            sb.append(content.substring(index));
            if(count>0){
                textArea.setText(sb.toString());
            }
        }
        if(count==0){
            showNotFoundAlert(keyword);
        }
        else {
            showReplaceCountAlert(count);
        }


    }

    private void showReplaceCountAlert(int count) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "已替换 " + count + " 处匹配项。");
        alert.setHeaderText(null);
        alert.show();
    }




    @FXML
    private void goTo() {//转到某一行
        //弹出输入框
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("转到行号");
        dialog.setHeaderText(null);
        dialog.setContentText("请输入行号：");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(lineStr -> {
            try {
                int lineNumber = Integer.parseInt(lineStr);
                if (lineNumber < 1) {
                    showAlert("行号不能小于 1！");
                    return;
                }

                // 获取所有行
                String[] lines = textArea.getText().split("\n");

                if (lineNumber > lines.length) {
                    showAlert("超出最大行数：" + lines.length);
                    return;
                }

                // 计算该行开始位置的字符索引
                int caretPos = 0;
                for (int i = 0; i < lineNumber - 1; i++) {
                    caretPos += lines[i].length() + 1; // 加上换行符
                }

                // 设置光标位置
                textArea.positionCaret(caretPos);
                textArea.requestFocus();

                // 更新状态栏
                updateStatus(null);
            } catch (NumberFormatException e) {
                showAlert("请输入有效的数字！");
            }
        });

    }

    @FXML
    private void zoomIn() {
        System.out.println("ok");
        Font currentFont = textArea.getFont();
        double newSize = currentFont.getSize() + 2;  // 每次放大2个单位
        textArea.setFont(Font.font(currentFont.getFamily(), newSize));
        updateStatus(null);  // 更新状态栏，确保字符数等信息是最新的

    }

    @FXML
    private void zoomOut() {
        Font currentFont = textArea.getFont();
        double newSize = currentFont.getSize() - 2;  // 每次缩小2个单位
        if (newSize > 2) {  // 限制最小字体大小为2
            textArea.setFont(Font.font(currentFont.getFamily(), newSize));
        }
        updateStatus(null);  // 更新状态栏，确保字符数等信息是最新的

    }

    @FXML
    private void resetZoom() {
        // 设置为默认字体大小
        Font currentFont = textArea.getFont();
        double defaultSize = 12;  // 你可以根据实际需求调整默认字体大小
        textArea.setFont(Font.font(currentFont.getFamily(), defaultSize));
        updateStatus(null);  // 更新状态栏，确保字符数等信息是最新的

    }

    @FXML
    private void insertDateTime() {
        // 获取当前系统时间
        LocalDateTime now = LocalDateTime.now();

        // 定义时间格式，2025/05/14 22:30
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        // 将时间格式化为字符串
        String dateTimeStr = now.format(formatter);

        // 获取当前光标位置
        int caretPos = textArea.getCaretPosition();

        // 在光标位置插入时间字符串
        textArea.insertText(caretPos, dateTimeStr);

        // 插入后将光标移动到时间后面
        textArea.positionCaret(caretPos + dateTimeStr.length());

        // 更新状态栏
        updateStatus(null);

    }

    @FXML
    private void toggleStatusBar() {
        boolean currentlyVisible = statusBar.isVisible();
        statusBar.setVisible(!currentlyVisible);
        statusBar.setManaged(!currentlyVisible); // 不显示时也不占用布局空间

    }

    @FXML
    private void setFont() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("设置字体");

        // 创建字体名称下拉框
        ComboBox<String> fontFamilyBox = new ComboBox<>();
        fontFamilyBox.getItems().addAll(Font.getFamilies());
        fontFamilyBox.setValue(textArea.getFont().getFamily());

        // 创建字号选择框
        Spinner<Integer> fontSizeSpinner = new Spinner<>(8, 72, (int) textArea.getFont().getSize());

        // 创建样式选择
        CheckBox boldCheckBox = new CheckBox("加粗");
        CheckBox italicCheckBox = new CheckBox("斜体");

        VBox content = new VBox(10, new Label("字体："), fontFamilyBox,
                new Label("字号："), fontSizeSpinner,
                boldCheckBox, italicCheckBox);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String family = fontFamilyBox.getValue();
            int size = fontSizeSpinner.getValue();
            FontWeight weight = boldCheckBox.isSelected() ? FontWeight.BOLD : FontWeight.NORMAL;
            FontPosture posture = italicCheckBox.isSelected() ? FontPosture.ITALIC : FontPosture.REGULAR;

            textArea.setFont(Font.font(family, weight, posture, size));
        }

    }



}
