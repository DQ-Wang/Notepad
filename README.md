# 5.6
（1）.实现对删除的监听，未选中文本时不可选

# 5.8

1.实现了查找&替换的UI

  了解了组件可以通过layoutX/layoutX改变组件在父容器中的的位置，AnchorPane有特殊的方式，但是我试了没效果

# 5.12

1.完善`find()`&`findNext()`&`finPrec()`代码块，可以选择是否实现**回绕**和**区分大小写**



# 5.14

1.完成`replace()`

* 可设置是否区分大小写

2.完成`replaceAll()`

* 不区分大小写时用正则方法，统一转换为小写容易出差错
* 区分大小写时用indeOf方法

3.实现`goTo()`转到特定行

4.完成状态栏在`findNext()`&`findPrevious()`的更新：调用`updateStatus(null)`

5.`zoomIn()`&`zoomOut()`&后序所有功能（插入日期，改变字体，显示状态栏）
