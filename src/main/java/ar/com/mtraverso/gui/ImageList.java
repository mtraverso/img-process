package ar.com.mtraverso.gui;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.Component;
import java.awt.Image;
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
            JLabel label = new JLabel(new ImageIcon((Image) value));
            return label;
        }
    }

    public class ImageListModel implements ListModel{
        List<Image> images = new ArrayList<Image>();
        @Override
        public int getSize() {
            return images.size();
        }

        @Override
        public Object getElementAt(int index) {
            return images.get(index);
        }

        public void removeElement() {
             images.remove(0);
        }

        public void addElement(Image image) {
            images.add(image);
        }

        @Override
        public void addListDataListener(ListDataListener l) {

        }

        @Override
        public void removeListDataListener(ListDataListener l) {

        }

        public void setImages(List<Image> images) {
            this.images = images;
        }
    }

    private int size;
    private ImageListModel model;

    public ImageList(int size){
        super();
        this.size = size;
        this.setCellRenderer(new ListRenderer());
        model = new ImageListModel();
        this.setModel(model);
    }

    public void add(Image i){
        if(getModel().getSize() > size){
            model.removeElement();
        }
        model.addElement(i);
    }
}
