package xyz.mcex.plugin.gui;

import xyz.mcex.plugin.internals.Nullable;

import java.util.Stack;

public class MenuFlow
{
  private Panel _currentPanel;
  private Stack<Panel> _panels;

  public MenuFlow(Panel defaultPanel)
  {
    this._currentPanel = defaultPanel;
  }

  public void switchPanel(@Nullable Panel panel)
  {
    if (panel == null)
      return;

    this._currentPanel.change(panel);
    this._currentPanel = panel;
  }

  public void pushToStack()
  {
    this._panels.push(this._currentPanel);
  }

  public Panel popFromStack()
  {
    if (this._panels.isEmpty())
      return null;
    return this._panels.pop();
  }
}
