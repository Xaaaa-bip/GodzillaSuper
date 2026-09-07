package util;

import core.ApplicationContext;
import core.ui.component.annotation.ButtonToMenuItem;
import core.ui.component.annotation.ClickSyncAnnotation;
import core.ui.component.dialog.GOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;

public class automaticBindClick {
    public automaticBindClick() {
    }

    public static void bindJButtonClick(Class<?> fieldClass, Object fieldObject, Class<?> eventClass, Object eventObject) {
        try {
            Field[] fields = fieldClass.getDeclaredFields();
            for (Field field : fields) {
                if (!field.getType().isAssignableFrom(JButton.class)) {
                    continue;
                }
                field.setAccessible(true);
                ClickSyncAnnotation clickSyncAnnotation = field.getAnnotation(ClickSyncAnnotation.class);
                JButton fieldValue = (JButton) field.get(fieldObject);
                String fieldName = field.getName();
                if (fieldValue == null) {
                    continue;
                }
                try {
                    Method method = eventClass.getDeclaredMethod(fieldName + "Click", ActionEvent.class);
                    method.setAccessible(true);
                    if (clickSyncAnnotation == null) {
                        clickSyncAnnotation = method.getAnnotation(ClickSyncAnnotation.class);
                    }
                    fieldValue.addActionListener(new GuardedClickListener(clickSyncAnnotation, method, eventObject));
                } catch (NoSuchMethodException e) {
                    Log.error(fieldName + "Click  未实现");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void bindJButtonClick(Object fieldClass, Object eventClass) {
        bindJButtonClick(fieldClass.getClass(), fieldClass, eventClass.getClass(), eventClass);
    }

    public static void bindMenuItemClick(Object item, Map<String, Method> methodMap, Object eventClass) {
        MenuElement[] menuElements = ((MenuElement) item).getSubElements();
        if (methodMap == null) {
            methodMap = getMenuItemMethod(eventClass);
        }
        if (menuElements.length == 0) {
            if (item.getClass().isAssignableFrom(JMenuItem.class)) {
                Method method = methodMap.get(((JMenuItem) item).getActionCommand() + "MenuItemClick");
                addMenuItemClickEvent(item, method, eventClass);
            }
            return;
        }
        for (int i = 0; i < menuElements.length; ++i) {
            MenuElement menuElement = menuElements[i];
            Class<?> itemClass = menuElement.getClass();
            if (itemClass.isAssignableFrom(JPopupMenu.class) || itemClass.isAssignableFrom(JMenu.class)) {
                bindMenuItemClick(menuElement, methodMap, eventClass);
            } else if (item.getClass().isAssignableFrom(JMenuItem.class)) {
                Method method = methodMap.get(((JMenuItem) menuElement).getActionCommand() + "MenuItemClick");
                addMenuItemClickEvent(menuElement, method, eventClass);
            }
        }
    }

    public static void bindButtonToMenuItem(Object fieldClass, Object eventClass, Object menu) {
        try {
            if (!JMenu.class.isAssignableFrom(menu.getClass()) && !JPopupMenu.class.isAssignableFrom(menu.getClass())) {
                return;
            }
            try {
                Field[] fields = fieldClass.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (!field.getType().isAssignableFrom(JButton.class)) {
                        continue;
                    }
                    field.setAccessible(true);
                    JButton fieldValue = (JButton) field.get(fieldClass);
                    String fieldName = field.getName();
                    if (fieldValue == null || !field.isAnnotationPresent(ButtonToMenuItem.class)) {
                        continue;
                    }
                    try {
                        ButtonToMenuItem buttonToMenuItem = field.getAnnotation(ButtonToMenuItem.class);
                        ClickSyncAnnotation clickSyncAnnotation = field.getAnnotation(ClickSyncAnnotation.class);
                        Method method = eventClass.getClass().getDeclaredMethod(fieldName + "Click", ActionEvent.class);
                        method.setAccessible(true);
                        if (clickSyncAnnotation == null) {
                            clickSyncAnnotation = method.getAnnotation(ClickSyncAnnotation.class);
                        }
                        Method addMethod = menu.getClass().getMethod("add", JMenuItem.class);
                        String menuItemName = fieldValue.getText();
                        String shown = buttonToMenuItem.name().length() > 0 ? buttonToMenuItem.name() : menuItemName;
                        JMenuItem menuItem = new JMenuItem(shown);
                        menuItem.addActionListener(new GuardedClickListener(clickSyncAnnotation, method, fieldClass));
                        addMethod.invoke(menu, menuItem);
                    } catch (NoSuchMethodException e) {
                        Log.error(fieldName + "Click  未实现");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }

    private static Map<String, Method> getMenuItemMethod(Object eventClass) {
        Method[] methods = eventClass.getClass().getDeclaredMethods();
        Map<String, Method> methodMap = new HashMap<String, Method>();
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1
                    && parameterTypes[0].isAssignableFrom(ActionEvent.class)
                    && method.getReturnType().isAssignableFrom(Void.TYPE)
                    && method.getName().endsWith("MenuItemClick")) {
                method.setAccessible(true);
                methodMap.put(method.getName(), method);
            }
        }
        return methodMap;
    }

    private static void addMenuItemClickEvent(Object item, Method method, Object eventClass) {
        if (method != null && eventClass != null && item.getClass().isAssignableFrom(JMenuItem.class)) {
            ((JMenuItem) item).addActionListener(new MenuItemClickListener(method, eventClass));
        }
    }

    /**
     * Keep the running flag until the click handler actually finishes.
     * The old code reset it right after Thread.start(), so a second click
     * (or Enter after a dialog) launched the same action again.
     */
    private static final class GuardedClickListener implements ActionListener {
        private boolean run;
        private final ClickSyncAnnotation syncAnnotation;
        private final Method method;
        private final Object eventObject;

        GuardedClickListener(ClickSyncAnnotation syncAnnotation, Method method, Object eventObject) {
            this.syncAnnotation = syncAnnotation;
            this.method = method;
            this.eventObject = eventObject;
            this.run = false;
        }

        public void actionPerformed(ActionEvent e) {
            if (this.run) {
                GOptionPane.showMessageDialog(null, "正在运行请等待!");
                return;
            }
            this.run = true;
            if (this.syncAnnotation == null && ApplicationContext.asyncClick) {
                new Thread(() -> {
                    try {
                        this.method.invoke(this.eventObject, e);
                    } catch (Exception ex) {
                        Log.error(ex);
                    } finally {
                        this.run = false;
                    }
                }, "gsl5-click").start();
            } else {
                try {
                    this.method.invoke(this.eventObject, e);
                } catch (Exception ex) {
                    Log.error(ex);
                } finally {
                    this.run = false;
                }
            }
        }
    }

    private static final class MenuItemClickListener implements ActionListener {
        private final Method method;
        private final Object eventObject;

        MenuItemClickListener(Method method, Object eventObject) {
            this.method = method;
            this.eventObject = eventObject;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                this.method.setAccessible(true);
                this.method.invoke(this.eventObject, e);
            } catch (Exception ex) {
                Log.error(ex);
            }
        }
    }
}
