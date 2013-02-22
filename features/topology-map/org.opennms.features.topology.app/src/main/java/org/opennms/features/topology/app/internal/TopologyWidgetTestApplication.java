/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.topology.app.internal;

import java.util.Iterator;
import java.util.List;

import org.opennms.features.topology.api.GraphContainer;
import org.opennms.features.topology.api.HistoryManager;
import org.opennms.features.topology.api.IViewContribution;
import org.opennms.features.topology.api.MapViewManager;
import org.opennms.features.topology.api.MapViewManagerListener;
import org.opennms.features.topology.api.SelectionManager;
import org.opennms.features.topology.api.WidgetContext;
import org.opennms.features.topology.api.topo.GraphProvider;
import org.opennms.features.topology.app.internal.TopoContextMenu.TopoContextMenuItem;
import org.opennms.features.topology.app.internal.TopologyComponent.VertexUpdateListener;
import org.opennms.features.topology.app.internal.jung.FRLayoutAlgorithm;
import org.opennms.features.topology.app.internal.support.IconRepositoryManager;

import com.github.wolfie.refresher.Refresher;
import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.terminal.Sizeable;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Slider;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UriFragmentUtility;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.UriFragmentUtility.FragmentChangedEvent;
import com.vaadin.ui.UriFragmentUtility.FragmentChangedListener;

public class TopologyWidgetTestApplication extends Application implements CommandUpdateListener, MenuItemUpdateListener, ContextMenuHandler, WidgetUpdateListener, WidgetContext, FragmentChangedListener, GraphContainer.ChangeListener, MapViewManagerListener, VertexUpdateListener {
    
    
	private static final long serialVersionUID = 6837501987137310938L;

	private static final String LABEL_PROPERTY = "label";
	private Window m_window;
	private TopologyComponent m_topologyComponent;
	private VertexSelectionTree m_tree;
	private final GraphContainer m_graphContainer;
	private final CommandManager m_commandManager;
	private MenuBar m_menuBar;
	private TopoContextMenu m_contextMenu;
	private AbsoluteLayout m_layout;
	private AbsoluteLayout m_rootLayout;
	private final IconRepositoryManager m_iconRepositoryManager;
	private WidgetManager m_widgetManager;
	private WidgetManager m_treeWidgetManager;
	private Accordion m_treeAccordion;
    private HorizontalSplitPanel m_treeMapSplitPanel;
    private VerticalSplitPanel m_bottomLayoutBar;
    private final Label m_zoomLevelLabel = new Label("0"); 
    private UriFragmentUtility m_uriFragUtil;
    private final HistoryManager m_historyManager;
    private final SelectionManager m_selectionManager;

	public TopologyWidgetTestApplication(CommandManager commandManager, HistoryManager historyManager, GraphProvider topologyProvider, ProviderManager providerManager, IconRepositoryManager iconRepoManager, SelectionManager selectionManager) {
		m_commandManager = commandManager;
		m_commandManager.addMenuItemUpdateListener(this);
		m_historyManager = historyManager;
		m_iconRepositoryManager = iconRepoManager;
		m_selectionManager = selectionManager;

		// Create a per-session GraphContainer instance
		m_graphContainer = new VEProviderGraphContainer(topologyProvider, providerManager);
		m_graphContainer.addChangeListener(this);
		m_graphContainer.getMapViewManager().addListener(this);
	}


	@SuppressWarnings("serial")
	@Override
	public void init() {
	    setTheme("topo_default");
	    
	    m_rootLayout = new AbsoluteLayout();
	    m_rootLayout.setSizeFull();
	    
	    m_window = new Window("OpenNMS Topology");
        m_window.setContent(m_rootLayout);
        setMainWindow(m_window);
        
        m_uriFragUtil = new UriFragmentUtility();
        m_window.addComponent(m_uriFragUtil);
        m_uriFragUtil.addListener(this);
	    
		m_layout = new AbsoluteLayout();
		m_layout.setSizeFull();
		m_rootLayout.addComponent(m_layout);

		Refresher refresher = new Refresher();
		refresher.setRefreshInterval(5000);
		getMainWindow().addComponent(refresher);

		m_graphContainer.setLayoutAlgorithm(new FRLayoutAlgorithm());

		final Property scale = m_graphContainer.getScaleProperty();

		m_topologyComponent = new TopologyComponent(m_graphContainer, m_iconRepositoryManager, m_selectionManager, this);
		m_topologyComponent.setSizeFull();
		m_topologyComponent.addMenuItemStateListener(this);
		m_topologyComponent.addVertexUpdateListener(this);
		
		final Slider slider = new Slider(0, 1);
		
		slider.setPropertyDataSource(scale);
		slider.setResolution(1);
		slider.setHeight("300px");
		slider.setOrientation(Slider.ORIENTATION_VERTICAL);

		slider.setImmediate(true);

		final Button zoomInBtn = new Button();
		zoomInBtn.setIcon(new ThemeResource("images/plus.png"));
		zoomInBtn.setDescription("Expand Semantic Zoom Level");
		zoomInBtn.setStyleName("semantic-zoom-button");
		zoomInBtn.addListener(new ClickListener() {

            public void buttonClick(ClickEvent event) {
				int szl = (Integer) m_graphContainer.getSemanticZoomLevel();
				szl++;
				m_graphContainer.setSemanticZoomLevel(szl);
				setSemanticZoomLevel(szl);
				saveHistory();
			}
		});

		Button zoomOutBtn = new Button();
		zoomOutBtn.setIcon(new ThemeResource("images/minus.png"));
		zoomOutBtn.setDescription("Collapse Semantic Zoom Level");
		zoomOutBtn.setStyleName("semantic-zoom-button");
		zoomOutBtn.addListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				int szl = (Integer) m_graphContainer.getSemanticZoomLevel();
				if(szl > 0) {
				    szl--;
				    m_graphContainer.setSemanticZoomLevel(szl);
				    setSemanticZoomLevel(szl);
				    saveHistory();
				} 
				
			}
		});
		
		
		final Button panBtn = new Button();
		panBtn.setIcon(new ThemeResource("images/cursor_drag_arrow.png"));
		panBtn.setDescription("Pan Tool");
		panBtn.setStyleName("toolbar-button down");
		
		final Button selectBtn = new Button();
		selectBtn.setIcon(new ThemeResource("images/selection.png"));
		selectBtn.setDescription("Selection Tool");
		selectBtn.setStyleName("toolbar-button");
		selectBtn.addListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                selectBtn.setStyleName("toolbar-button down");
                panBtn.setStyleName("toolbar-button");
                m_topologyComponent.setActiveTool("select");
            }
        });
		
		panBtn.addListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                panBtn.setStyleName("toolbar-button down");
                selectBtn.setStyleName("toolbar-button");
                m_topologyComponent.setActiveTool("pan");
            }
        });
		
		VerticalLayout toolbar = new VerticalLayout();
		toolbar.setWidth("31px");
		toolbar.addComponent(panBtn);
		toolbar.addComponent(selectBtn);
		
		HorizontalLayout semanticLayout = new HorizontalLayout();
		semanticLayout.addComponent(zoomInBtn);
		semanticLayout.addComponent(m_zoomLevelLabel);
		semanticLayout.addComponent(zoomOutBtn);
		semanticLayout.setComponentAlignment(m_zoomLevelLabel, Alignment.MIDDLE_CENTER);
		
		AbsoluteLayout mapLayout = new AbsoluteLayout();

		mapLayout.addComponent(m_topologyComponent, "top:0px; left: 0px; right: 0px; bottom: 0px;");
		mapLayout.addComponent(slider, "top: 5px; left: 20px; z-index:1000;");
		mapLayout.addComponent(toolbar, "top: 324px; left: 12px;");
		mapLayout.addComponent(semanticLayout, "top: 380px; left: 2px;");
		mapLayout.setSizeFull();

		m_treeMapSplitPanel = new HorizontalSplitPanel();
		m_treeMapSplitPanel.setFirstComponent(createWestLayout());
		m_treeMapSplitPanel.setSecondComponent(mapLayout);
		m_treeMapSplitPanel.setSplitPosition(222, Sizeable.UNITS_PIXELS);
		m_treeMapSplitPanel.setSizeFull();

		m_commandManager.addCommandUpdateListener(this);

		menuBarUpdated(m_commandManager);
		if(m_widgetManager.widgetCount() != 0) {
		    updateWidgetView(m_widgetManager);
		}else {
		    m_layout.addComponent(m_treeMapSplitPanel, "top: 23px; left: 0px; right:0px; bottom:0px;");
		}
		
		if(m_treeWidgetManager.widgetCount() != 0) {
		    updateAccordionView(m_treeWidgetManager);
		}
	}

	/**
	 * Update the Accordion View with installed widgets
	 * @param treeWidgetManager
	 */
    private void updateAccordionView(WidgetManager treeWidgetManager) {
        m_treeAccordion.removeAllComponents();
        
        m_treeAccordion.addTab(m_tree, m_tree.getTitle());
        for(IViewContribution widget : treeWidgetManager.getWidgets()) {
            if(widget.getIcon() != null) {
                m_treeAccordion.addTab(widget.getView(this), widget.getTitle(), widget.getIcon());
            }else {
                m_treeAccordion.addTab(widget.getView(this), widget.getTitle());
            }
        }
    }

    /**
     * Updates the bottom widget area with the registered widgets
     * 
     * Any widget with the service property of 'location=bottom' are
     * included.
     * 
     * @param widgetManager
     */
    private void updateWidgetView(WidgetManager widgetManager) {
        if(widgetManager.widgetCount() == 0) {
            m_layout.removeAllComponents();
            m_layout.addComponent(m_treeMapSplitPanel, "top: 23px; left: 0px; right:0px; bottom:0px;");
            m_layout.requestRepaint();
        } else {
            if(m_bottomLayoutBar == null) {
                m_bottomLayoutBar = new VerticalSplitPanel();
                m_bottomLayoutBar.setFirstComponent(m_treeMapSplitPanel);
                // Split the screen 70% top, 30% bottom
                m_bottomLayoutBar.setSplitPosition(70, Sizeable.UNITS_PERCENTAGE);
                m_bottomLayoutBar.setSizeFull();
                m_bottomLayoutBar.setSecondComponent(widgetManager.getTabSheet(this));
            }

            m_layout.removeAllComponents();
            m_layout.addComponent(m_bottomLayoutBar, "top: 23px; left: 0px; right:0px; bottom:0px;");
            m_layout.requestRepaint();
            
        }
        
        if(m_contextMenu != null && m_contextMenu.getParent() == null) {
            getMainWindow().addComponent(m_contextMenu);
        }
    }

    /**
     * Creates the west area layout including the
     * accordion and tree views.
     * 
     * @return
     */
    @SuppressWarnings("serial")
	private Layout createWestLayout() {
        m_tree = createTree();
        
        
        final TextField filterField = new TextField("Filter");
        filterField.setTextChangeTimeout(200);
        
        
        final Button filterBtn = new Button("Filter");
        filterBtn.addListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
            	GCFilterableContainer container = m_tree.getContainerDataSource();
                container.removeAllContainerFilters();
                
                String filterString = (String) filterField.getValue();
                if(!filterString.equals("") && filterBtn.getCaption().toLowerCase().equals("filter")) {
                    container.addContainerFilter(LABEL_PROPERTY, (String) filterField.getValue(), true, false);
                    filterBtn.setCaption("Clear");
                } else {
                    filterField.setValue("");
                    filterBtn.setCaption("Filter");
                }
                
            }
        });
        
        
        HorizontalLayout filterArea = new HorizontalLayout();
        filterArea.addComponent(filterField);
        filterArea.addComponent(filterBtn);
        filterArea.setComponentAlignment(filterBtn, Alignment.BOTTOM_CENTER);
        
        m_treeAccordion = new Accordion();
        m_treeAccordion.addTab(m_tree, m_tree.getTitle());
        m_treeAccordion.setWidth("100%");
        m_treeAccordion.setHeight("100%");
        
        AbsoluteLayout absLayout = new AbsoluteLayout();
        absLayout.setWidth("100%");
        absLayout.setHeight("100%");
        absLayout.addComponent(filterArea, "top: 25px; left: 15px;");
        absLayout.addComponent(m_treeAccordion, "top: 75px; left: 15px; right: 15px; bottom:25px;"); 
        
        return absLayout;
    }

    private VertexSelectionTree createTree() {
	    //final FilterableHierarchicalContainer container = new FilterableHierarchicalContainer(m_graphContainer.getVertexContainer());
	    
		VertexSelectionTree tree = new VertexSelectionTree("Nodes", m_graphContainer, m_selectionManager);
		tree.setMultiSelect(true);
		tree.setImmediate(true);
		tree.setItemCaptionPropertyId(LABEL_PROPERTY);

		for (Iterator<?> it = tree.rootItemIds().iterator(); it.hasNext();) {
			Object item = it.next();
			tree.expandItemsRecursively(item);
		}
		
		m_selectionManager.addSelectionListener(tree);

		return tree;
	}

	@Override
	public void updateMenuItems() {
		updateMenuItems(m_menuBar.getItems());
		m_menuBar.requestRepaint();
	}

	private void updateContextMenuItems(Object target, List<TopoContextMenuItem> items) {
		for(TopoContextMenuItem contextItem : items) {
			if(contextItem.hasChildren()) {
				updateContextMenuItems(target, contextItem.getChildren());
			} else {
				m_commandManager.updateContextMenuItem(target, contextItem, m_graphContainer, getMainWindow());
			}
		}
	}


	private void updateMenuItems(List<MenuItem> menuItems) {
		for(MenuItem menuItem : menuItems) {
			if(menuItem.hasChildren()) {
				updateMenuItems(menuItem.getChildren());
			}else {
				m_commandManager.updateMenuItem(menuItem, m_graphContainer, getMainWindow(), m_selectionManager);
			}
		}
	}

	@Override
	public void menuBarUpdated(CommandManager commandManager) {
		if(m_menuBar != null) {
			m_rootLayout.removeComponent(m_menuBar);
		}

		if(m_contextMenu != null) {
			getMainWindow().removeComponent(m_contextMenu);
		}

		m_menuBar = commandManager.getMenuBar(m_graphContainer, getMainWindow(), m_selectionManager);
		m_menuBar.setWidth("100%");
		m_rootLayout.addComponent(m_menuBar, "top: 0px; left: 0px; right:0px;");

		m_contextMenu = commandManager.getContextMenu(m_graphContainer, getMainWindow());
		getMainWindow().addComponent(m_contextMenu);
		updateMenuItems();
	}
	
	public void show(Object target, int left, int top) {
		updateContextMenuItems(target, m_contextMenu.getItems());
		updateSubMenuDisplay(m_contextMenu.getItems());
		m_contextMenu.setTarget(target);
		m_contextMenu.show(left, top);
	}


	private void updateSubMenuDisplay(List<TopoContextMenuItem> items) {
		for (TopoContextMenuItem item : items) {
			if (!item.hasChildren()) continue;
			else updateSubMenuDisplay(item.getChildren());
			boolean shouldDisplay = false;
			for (TopoContextMenuItem child : item.getChildren()) {
				if (child.getItem().isVisible()) {
					shouldDisplay = true;
					break;
				}
			}
			item.getItem().setVisible(shouldDisplay);
		}
	}


    public WidgetManager getWidgetManager() {
        return m_widgetManager;
    }


    public void setWidgetManager(WidgetManager widgetManager) {
        if(m_widgetManager != null) {
            m_widgetManager.removeUpdateListener(this);
        }
        m_widgetManager = widgetManager;
        m_widgetManager.addUpdateListener(this);
    }


    @Override
    public void widgetListUpdated(WidgetManager widgetManager) {
        if(isRunning()) {
            if(widgetManager == m_widgetManager) {
                updateWidgetView(widgetManager);
            }else if(widgetManager == m_treeWidgetManager) {
                updateAccordionView(widgetManager);
            }
        }
    }


    public WidgetManager getTreeWidgetManager() {
        return m_treeWidgetManager;
    }


    public void setTreeWidgetManager(WidgetManager treeWidgetManager) {
        if(m_treeWidgetManager != null) {
            m_treeWidgetManager.removeUpdateListener(this);
        }
        
        m_treeWidgetManager = treeWidgetManager;
        m_treeWidgetManager.addUpdateListener(this);
    }


    @Override
    public GraphContainer getGraphContainer() {
        return m_graphContainer;
    }


    int m_settingFragment = 0;
    @Override
    public void fragmentChanged(FragmentChangedEvent source) {
        m_settingFragment++;
        String fragment = source.getUriFragmentUtility().getFragment();
        m_historyManager.applyHistory(fragment, m_graphContainer);
        m_settingFragment--;
    }


    private void saveHistory() {
        if (m_settingFragment == 0) {
            String fragment = m_historyManager.create(m_graphContainer);
            m_uriFragUtil.setFragment(fragment, false);
        }
    }


    @Override
    public void graphChanged(GraphContainer graphContainer) {
        m_zoomLevelLabel.setValue("" + graphContainer.getSemanticZoomLevel());
    }


    private void setSemanticZoomLevel(int szl) {
        m_zoomLevelLabel.setValue(szl);
        m_graphContainer.redoLayout();
    }


    @Override
    public void boundingBoxChanged(MapViewManager viewManager) {
        saveHistory();
    }


    @Override
    public void onVertexUpdate() {
        saveHistory();
    }


}
