package com.vaadin.tests.components.orderedlayout;

import java.util.ArrayList;
import java.util.Arrays;

import com.vaadin.annotations.Theme;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.terminal.UserError;
import com.vaadin.terminal.WrappedRequest;
import com.vaadin.tests.components.AbstractTestRoot;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Label.ContentMode;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

@Theme("tests-components")
public class BoxLayoutTest extends AbstractTestRoot {

    protected AbstractOrderedLayout view;

    protected AbstractOrderedLayout l;

    protected AbstractComponent target;

    protected NativeSelect componentWidth;
    protected NativeSelect componentHeight;
    protected NativeSelect componentCaption;
    protected NativeSelect componentIcon;
    protected TextField componentDescription;
    protected CheckBox componentError;

    protected NativeSelect align;
    protected CheckBox expand;

    @Override
    protected void setup(WrappedRequest request) {

        view = new VerticalLayout();
        view.setSizeFull();
        view.setMargin(true);
        view.setSpacing(true);

        view.addComponent(createControls(false));
        view.addComponent(createTestLayout(false));
        view.setExpandRatio(view.getComponent(1), 1);

        setContent(view);
        getApplication().setRootPreserved(true);
    }

    protected AbstractOrderedLayout createControls(boolean horizontal) {
        VerticalLayout root = new VerticalLayout();
        root.setSpacing(true);

        // First row
        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(true);
        root.addComponent(header);

        Label title = new Label("BoxLayout Test");
        title.addStyleName(Reindeer.LABEL_H1);
        header.addComponent(title);

        final CheckBox vertical = new CheckBox("Vertical", !horizontal);
        vertical.setImmediate(true);
        vertical.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                view.removeAllComponents();

                view.addComponent(createControls(!vertical.getValue()
                        .booleanValue()));
                view.addComponent(createTestLayout(!vertical.getValue()
                        .booleanValue()));

                view.setExpandRatio(view.getComponent(1), 1);

            }
        });
        header.addComponent(vertical);

        Button addComponent = new Button("Add Component",
                new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        GridLayout grid = new GridLayout(2, 2);
                        Button grow = new Button("Grow Me",
                                new Button.ClickListener() {
                                    public void buttonClick(ClickEvent event) {
                                        if (event.getButton().getWidth() == -1) {
                                            event.getButton().setHeight("50px");
                                            event.getButton().setWidth("200px");
                                        } else {
                                            event.getButton()
                                                    .setSizeUndefined();
                                        }
                                    }
                                });
                        grid.addComponent(new Label("Grid cell 1"));
                        grid.addComponent(new Label("Grid cell 2"));
                        grid.addComponent(grow);
                        grid.addComponent(new Label("Grid cell 4"));
                        l.addComponent(grid);
                    }
                });
        header.addComponent(addComponent);

        Button removeComponent = new Button("Remove Component",
                new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        Component last = l.getComponent(l.getComponentCount() - 1);
                        l.removeComponent(last);
                    }
                });
        header.addComponent(removeComponent);

        // Second row
        HorizontalLayout controls = new HorizontalLayout();
        controls.setSpacing(true);
        root.addComponent(controls);

        // Layout controls
        HorizontalLayout layout = new HorizontalLayout();
        layout.addStyleName("fieldset");
        layout.setSpacing(true);
        controls.addComponent(layout);
        layout.addComponent(new Label("Layout"));

        ArrayList<String> sizes = new ArrayList<String>();
        sizes.addAll(Arrays.asList("100px", "30em", "100%"));

        final NativeSelect width = new NativeSelect(null, sizes);
        width.setImmediate(true);
        width.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                if (width.getValue() != null) {
                    l.setWidth(width.getValue().toString());
                } else {
                    l.setWidth(null);
                }
            }
        });
        layout.addComponent(width);
        layout.addComponent(new Label("&times;", ContentMode.XHTML));
        final NativeSelect height = new NativeSelect(null, sizes);
        height.setImmediate(true);
        height.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                if (height.getValue() != null) {
                    l.setHeight(height.getValue().toString());
                } else {
                    l.setHeight(null);
                }
            }
        });
        layout.addComponent(height);

        final CheckBox margin = new CheckBox("Margin", false);
        margin.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                l.setMargin(margin.getValue().booleanValue());
            }
        });
        margin.setImmediate(true);
        layout.addComponent(margin);
        layout.addComponent(margin);

        final CheckBox spacing = new CheckBox("Spacing", false);
        spacing.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                l.setSpacing(spacing.getValue().booleanValue());
            }
        });
        spacing.setImmediate(true);
        layout.addComponent(spacing);

        // Cell controls
        HorizontalLayout cell = new HorizontalLayout();
        cell.addStyleName("fieldset");
        cell.setSpacing(true);
        controls.addComponent(cell);
        cell.addComponent(new Label("Cell"));

        ArrayList<Alignment> alignments = new ArrayList<Alignment>();
        alignments.addAll(Arrays.asList(Alignment.TOP_LEFT,
                Alignment.MIDDLE_LEFT, Alignment.BOTTOM_LEFT,
                Alignment.TOP_CENTER, Alignment.MIDDLE_CENTER,
                Alignment.BOTTOM_CENTER, Alignment.TOP_RIGHT,
                Alignment.MIDDLE_RIGHT, Alignment.BOTTOM_RIGHT));

        align = new NativeSelect(null, alignments);
        for (Alignment a : alignments) {
            align.setItemCaption(a,
                    a.getVerticalAlignment() + "-" + a.getHorizontalAlignment());
        }
        align.setImmediate(true);
        align.setEnabled(false);
        align.setNullSelectionAllowed(false);
        align.select(Alignment.TOP_LEFT);
        align.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                if (target == null) {
                    return;
                }
                l.setComponentAlignment(target, ((Alignment) align.getValue()));
            }
        });
        cell.addComponent(align);

        expand = new CheckBox("Expand");
        expand.setImmediate(true);
        expand.setEnabled(false);
        expand.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                if (target != null) {
                    l.setExpandRatio(target, expand.getValue() ? 1 : 0);
                }
            }
        });
        cell.addComponent(expand);

        // Component controls
        HorizontalLayout component = new HorizontalLayout();
        component.addStyleName("fieldset");
        component.setSpacing(true);
        root.addComponent(component);
        component.addComponent(new Label("Component"));

        sizes = new ArrayList<String>();
        sizes.addAll(Arrays.asList("50px", "200px", "10em", "50%", "100%"));

        componentWidth = new NativeSelect(null, sizes);
        componentWidth.setImmediate(true);
        componentWidth.setEnabled(false);
        componentWidth.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                if (target == null) {
                    return;
                }
                if (componentWidth.getValue() != null) {
                    target.setWidth(componentWidth.getValue().toString());
                } else {
                    target.setWidth(null);
                }
            }
        });
        component.addComponent(componentWidth);
        component.addComponent(new Label("&times;", ContentMode.XHTML));

        componentHeight = new NativeSelect(null, sizes);
        componentHeight.setImmediate(true);
        componentHeight.setEnabled(false);
        componentHeight.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                if (componentHeight.getValue() != null) {
                    target.setHeight(componentHeight.getValue().toString());
                } else {
                    target.setHeight(null);
                }
            }
        });
        component.addComponent(componentHeight);

        componentCaption = new NativeSelect("Caption", Arrays.asList("Short",
                "Slightly Longer Caption"));
        componentCaption.setImmediate(true);
        componentCaption.setEnabled(false);
        componentCaption.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                if (componentCaption.getValue() != null) {
                    target.setCaption(componentCaption.getValue().toString());
                } else {
                    target.setCaption(null);
                }
            }
        });
        component.addComponent(componentCaption);

        componentIcon = new NativeSelect("Icon", Arrays.asList(
                "../runo/icons/16/folder.png", "../runo/icons/32/document.png"));
        componentIcon.setImmediate(true);
        componentIcon.setEnabled(false);
        componentIcon.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                if (componentIcon.getValue() != null) {
                    target.setIcon(new ThemeResource(componentIcon.getValue()
                            .toString()));
                } else {
                    target.setIcon(null);
                }
            }
        });
        component.addComponent(componentIcon);

        componentDescription = new TextField("Description");
        componentDescription.setImmediate(true);
        componentDescription.setEnabled(false);
        componentDescription.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                target.setDescription(componentDescription.getValue());
            }
        });
        component.addComponent(componentDescription);

        componentError = new CheckBox("Error");
        componentError.setImmediate(true);
        componentError.setEnabled(false);
        componentError.addListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                if (target != null) {
                    target.setComponentError(componentError.getValue() ? new UserError(
                            "Error message") : null);
                }
            }
        });
        component.addComponent(componentError);

        return root;
    }

    protected AbstractOrderedLayout createTestLayout(boolean horizontal) {
        l = horizontal ? new HorizontalLayout() : new VerticalLayout();
        l.setSizeUndefined();
        l.addStyleName("test");

        Label label = new Label("Component 1");
        l.addComponent(label);
        l.addComponent(new Button("Component 2"));

        l.addListener(new LayoutClickListener() {
            public void layoutClick(LayoutClickEvent event) {
                if (event.getChildComponent() == null
                        || target == event.getChildComponent()) {
                    if (target != null) {
                        target.removeStyleName("target");
                    }
                    target = null;
                } else if (target != event.getChildComponent()) {
                    if (target != null) {
                        target.removeStyleName("target");
                    }
                    target = (AbstractComponent) event.getChildComponent();
                    target.addStyleName("target");
                }
                componentWidth.setEnabled(target != null);
                componentHeight.setEnabled(target != null);
                componentCaption.setEnabled(target != null);
                componentIcon.setEnabled(target != null);
                componentDescription.setEnabled(target != null);
                componentError.setEnabled(target != null);
                align.setEnabled(target != null);
                expand.setEnabled(target != null);
                if (target != null) {
                    if (target.getWidth() > -1) {
                        componentWidth.select(new Float(target.getWidth())
                                .intValue()
                                + target.getWidthUnits().getSymbol());
                    } else {
                        componentWidth.select(null);
                    }
                    if (target.getHeight() > -1) {
                        componentHeight.select(new Float(target.getHeight())
                                .intValue()
                                + target.getHeightUnits().getSymbol());
                    } else {
                        componentHeight.select(null);
                    }

                    align.select(l.getComponentAlignment(target));
                    expand.setValue(new Boolean(l.getExpandRatio(target) > 0));

                    componentCaption.select(target.getCaption());
                    if (target.getIcon() != null) {
                        componentIcon.select(((ThemeResource) target.getIcon())
                                .getResourceId());
                    } else {
                        componentIcon.select(null);
                    }
                    componentDescription.setValue(target.getDescription());
                    componentError.setValue(target.getComponentError() != null);
                }
            }
        });

        target = null;

        return l;
    }

    @Override
    protected String getTestDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Integer getTicketNumber() {
        // TODO Auto-generated method stub
        return null;
    }

}