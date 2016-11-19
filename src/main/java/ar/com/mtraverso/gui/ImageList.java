package ar.com.mtraverso.gui;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by mtraverso on 11/15/16.
 */
public class ImageList extends JList{
    public class ListRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
            JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
            Component c = null;
            if (value == null) {
                c = new JLabel("(null)");
            } else if (value instanceof Component) {
                c = (Component) value;
            } else {
                c = new JLabel(new ImageIcon((BufferedImage)value));
            }
            if (c instanceof JComponent) {
                ((JComponent) c).setOpaque(true);
            }

            return c;
        }
    }

    private int size;
    private DefaultListModel<Image> model;

    public ImageList(int size){
        super();
        this.size = size;
        setCellRenderer(new ListRenderer());
        model = new DefaultListModel<Image>();
        this.setModel(model);
    }

    public void add(Image i){
        if(getModel().getSize() == size){
            model.removeElementAt(0);
        }
        model.addElement(i);
    }
}
