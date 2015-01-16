// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.hubnet.server

import org.nlogo.hubnet.mirroring.HubNetPlotPoint
import org.nlogo.workspace.AbstractWorkspaceScala
import org.nlogo.plot.Plot
import scala.collection.mutable.{ Publisher, Subscriber }
import org.nlogo.api.PlotAction, PlotAction._ 

// This should definitely be all redone.
// It sends messages out on every single little change.
// We really ought to only send bunched diffs the way we do with view updates.
// - JOSH COUGH 8/21/10, 12/28/10
class ServerPlotManager(workspace: AbstractWorkspaceScala, connectionManager: ConnectionManager,
                        plots: => List[Plot], currentPlot: => Plot) extends Subscriber[PlotAction, Publisher[PlotAction]] {
  private val narrowcastPlots = new collection.mutable.ListBuffer[String]()

  private def broadcastToClients(a: Any) { broadcastToClients(a, currentPlot.name) }
  private def broadcastToClients(a: Any, plotName: String) {
    if (broadcastEnabled && isBroadcast(plotName)) connectionManager.broadcastPlotControl(a,plotName)
  }
  private def broadcastWidgetToClients(a: Any, widgetName: String) {
    if (broadcastEnabled && isBroadcast(widgetName)) connectionManager.broadcast(widgetName, a)
  }

  private def sendToClient(clientId: String, a: Any) {
    if (workspace.hubNetRunning && isNarrowcast(currentPlot.name)) {
      connectionManager.sendPlotControl(clientId, a, currentPlot.name)
    }
  }

  private def broadcastEnabled = workspace.hubNetRunning && HubNetUtils.plotMirroring
  private def isNarrowcast(plotName: String) = narrowcastPlots.contains(plotName) && connectionManager.isValidTag(plotName)
  private def isBroadcast(plotName: String) = (!narrowcastPlots.contains(plotName)) && connectionManager.isValidTag(plotName)

  // called when plot mirroring is enabled to send all existing data
  // to all existing clients
  def broadcastPlots() {
    for(p<-plots; if (isBroadcast(p.name))) connectionManager.broadcast(p)
  }

  // called when a new client logs in and needs to be brought up to date
  def sendPlots(client:String) {
    if (broadcastEnabled) for(p<-plots; if (isBroadcast(p.name))) connectionManager.sendPlot(client, p)
  }

  /**
   * designates a particular plot as narrow-cast (by name).
   * @return true if the given plot exists, false otherwise.
   */
  def addNarrowcastPlot(plotName: String) = plots.find(_.name == plotName) match {
    case Some(plot) =>
      narrowcastPlots += plotName
      true
    case None => false
  }

  @Override def notify(pub: Publisher[PlotAction], action: PlotAction) : Unit = {
    action match {
      case PlotY(plotName, penName, y) => {
        val x = plots.find(_.name.equalsIgnoreCase(plotName)).flatMap(_.getPen(penName)).get.state.x
        broadcastToClients(penName, plotName)
        broadcastToClients(new HubNetPlotPoint(x, y), plotName)
      }
      case ClearPlot(plotName) => {}
      case PlotXY(plotName, penName, x, y) => {}
      case AutoPlot(plotName, on) => {}
      case SetRange(plotName, isX, min, max) => {}
      case PenDown(plotName, penName, down) => {}
      case HidePen(plotName, penName, hidden) => {}
      case HardResetPen(plotName, penName) => {}
      case SoftResetPen(plotName, penName) => {}
      case SetPenInterval(plotName, penName, interval) => {}
      case SetPenMode(plotName, penName, mode) => {}
      case SetPenColor(plotName, penName, color) => {}
      case CreateTemporaryPen(plotName, penName) => {}

    }
  }

  // Below are all the Plot Actions that are sent to clients

  // Sends the java.lang.Character 'a', indicating a clear-all-plots
  // ALL PLOTS is a special value so we don't need to find
  // a valid plot since we're not sending the message to a
  // particular plot.  This type of message is handled specially
  // like VIEW ev 9/9/08
  def clearAll() { broadcastWidgetToClients('a', "ALL PLOTS") }

  // Sends the java.lang.Character 'c', indicating a clear-plot
  def clear() {broadcastToClients('c')}
  // Sends without distinguishing default from temporary
  def defaultXMin(defaultXMin: Double) {xMin(defaultXMin)}
  def defaultYMin(defaultYMin: Double) {yMin(defaultYMin)}
  def defaultXMax(defaultXMax: Double) {xMax(defaultXMax)}
  def defaultYMax(defaultYMax: Double) {yMax(defaultYMax)}
  def defaultAutoPlotOn(defaultAutoPlotOn: Boolean) {autoPlotOn(defaultAutoPlotOn)}
  // Sends the java.lang.Character 'n' or 'f', indicating a auto-plot-on and auto-plot-off respectively
  def autoPlotOn(flag: Boolean) {if (flag) broadcastToClients('n') else broadcastToClients('f')}
  // Sends a java.lang.Short, which is the current plot-pen-mode
  def plotPenMode(plotPenMode: Int) {broadcastToClients(plotPenMode.toShort)}
  def plot(x: Double, y: Double) {broadcastToClients(new HubNetPlotPoint(x, y))}
  // Sends the org.nlogo.hubnet.HubNetPlotPoint plotted to a single client
  def narrowcastPlot(clientId: String, y: Double) {sendToClient(clientId, new HubNetPlotPoint(y))}
  // Sends the org.nlogo.hubnet.HubNetPlotPoint plotted to a single client
  def narrowcastPlot(clientId: String, x: Double, y: Double) {sendToClient(clientId, new HubNetPlotPoint(x, y))}
  def narrowcastClear(clientId: String) {sendToClient(clientId, 'c')}
  // Sends a java.lang.Boolean, which is the value of penDown
  def narrowcastPenDown(clientId: String, penDown: Boolean) {sendToClient(clientId, penDown)}
  def narrowcastPlotPenMode(clientId: String, plotPenMode: Int) {sendToClient(clientId, plotPenMode.toShort)}
  // Sends a java.lang.Double, which is the current plot-pen-interval
  def narrowcastSetInterval(clientId: String, interval: Double) {sendToClient(clientId, interval.toDouble)}
  def narrowcastSetHistogramNumBars(clientId: String, num: Int) {
    narrowcastSetInterval(clientId, (currentPlot.state.xMax - currentPlot.state.xMin) / num)
  }
  // Sends the java.lang.Character 'r' or 'p', indicating reset-plot-pen and whether to resetPoints
  def resetPen(resetPoints: Boolean) {if (resetPoints) broadcastToClients('r') else broadcastToClients('p')}
  // Sends a java.lang.Boolean, which is the value of penDown
  def penDown(penDown: Boolean) {broadcastToClients(penDown)}
  // Sends as a plot-pen-interval
  // TODO: I dont think this really works... Its ignoring the argument! - JC 8/21/10
  def setHistogramNumBars(num: Int) {for(pen <- currentPlot.currentPen) setInterval(pen.state.interval)}
  // TODO: Note - send strings to mean pen name, ints for color, doubles for interval, boolean for pen down...BAD!
  // Sends a java.lang.String, which is the name of the current plot pen
  def currentPen(penName: String) {broadcastToClients(penName)}
  // Sends the java.lang.Integer, which is the intValue of the java.awt.Color of the plot pen
  def setPenColor(color: Int) {broadcastToClients(color)}
  def setInterval(interval: Double) {broadcastToClients(interval)}
  def xRange(min: Double, max: Double) {broadcastToClients(List('x', min, max))}
  def yRange(min: Double, max: Double) {broadcastToClients(List('y', min, max))}
  def xMin(min: Double) {xRange(min, currentPlot.state.xMax)}
  def xMax(max: Double) {xRange(currentPlot.state.xMin, max)}
  def yMin(min: Double) {yRange(min, currentPlot.state.yMax)}
  def yMax(max: Double) {yRange(currentPlot.state.yMin, max)}
}
