/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   12.07.2006 (sieb): created
 */
package org.knime.workbench.editor2.editparts.snap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.handles.HandleBounds;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.knime.core.node.port.PortType;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.AbstractPortEditPart;
import org.knime.workbench.editor2.editparts.AbstractWorkflowPortBarEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortEditPart;

/**
 * A temporary helper used to perform snapping to existing elements. This helper
 * can be used in conjunction with the
 * {@link org.eclipse.gef.tools.DragEditPartsTracker DragEditPartsTracker} when
 * dragging editparts within a graphical viewer. Snapping is based on the
 * existing children of a <I>container</I>. When snapping a rectangle, the
 * edges of the rectangle will snap to edges of other rectangles generated from
 * the children of the given container. Similarly, the centers and middles of
 * rectangles will snap to each other.
 * <P>
 * If the snap request is being made during a Move, Reparent or Resize, then the
 * figures of the participants of that request will not be used for snapping. If
 * the request is a Clone, then the figures for the parts being cloned will be
 * used as possible snap locations.
 * <P>
 * This helper does not keep up with changes made to the container editpart.
 * Clients should instantiate a new helper each time one is requested and not
 * hold on to instances of the helper.
 *
 * @since 3.0
 * @author Randy Hudson
 * @author Pratik Shah
 */
public class SnapToPortGeometry extends SnapToHelper {

    /**
     * A property indicating whether this helper should be used. The value
     * should be an instance of Boolean. Currently, this class does not check to
     * see if the viewer property is set to <code>true</code>.
     *
     * @see org.eclipse.gef.EditPartViewer#setProperty(String, Object)
     */
    public static final String PROPERTY_SNAP_ENABLED = "SnapToGeometry.isEnabled"; //$NON-NLS-1$

    /**
     * The key used to identify the North anchor point in the extended data of a
     * request. The north anchor may be set to an {@link Integer} value
     * indicating where the snapping is occurring. This is used for feedback
     * purposes.
     */
    public static final String KEY_NORTH_ANCHOR = "SnapToGeometry.NorthAnchor"; //$NON-NLS-1$

    /**
     * The key used to identify the South anchor point in the extended data of a
     * request. The south anchor may be set to an {@link Integer} value
     * indicating where the snapping is occurring. This is used for feedback
     * purposes.
     */
    public static final String KEY_SOUTH_ANCHOR = "SnapToGeometry.SouthAnchor"; //$NON-NLS-1$

    /**
     * The key used to identify the West anchor point in the extended data of a
     * request. The west anchor may be set to an {@link Integer} value
     * indicating where the snapping is occurring. This is used for feedback
     * purposes.
     */
    public static final String KEY_WEST_ANCHOR = "SnapToGeometry.WestAnchor"; //$NON-NLS-1$

    /**
     * The key used to identify the East anchor point in the extended data of a
     * request. The east anchor may be set to an {@link Integer} value
     * indicating where the snapping is occurring. This is used for feedback
     * purposes.
     */
    public static final String KEY_EAST_ANCHOR = "SnapToGeometry.EastAnchor"; //$NON-NLS-1$

    /**
     * A vertical or horizontal snapping point. since 3.0
     */
    private static class Entry {
        /**
         * The side from which this entry was created. -1 is used to indicate
         * left or top, 0 indicates the middle or center, and 1 indicates right
         * or bottom.
         */
        int m_side;

        /**
         * The location of the entry, in the container's coordinates.
         */
        int m_offset;

        /**
         * Wheather this is an inport value.
         */
        boolean m_inport;

        /**
         * Type of this port.
         */
        PortType m_portType;

        /**
         * Constructs a new entry with the given side and offset.
         *
         * @param side an integer indicating T/L, B/R, or C/M
         * @param offset the location
         */
        Entry(final int side, final int offset) {
            this.m_side = side;
            this.m_offset = offset;
        }

        /**
         * Constructs a new entry with the given side and offset.
         *
         * @param side an integer indicating T/L, B/R, or C/M
         * @param offset the location
         * @param inport wheather this entry belongs to an inport
         * @param type the type of the port
         */
        Entry(final int side, final int offset, final boolean inport,
                final PortType type) {
            this.m_side = side;
            this.m_offset = offset;
            m_inport = inport;
            m_portType = type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Messages.SnapToPortGeometry_1 + m_offset + Messages.SnapToPortGeometry_2 + m_inport;
        }
    }

    /**
     * The sensitivity of the snapping. Corrections greater than this value will
     * not occur.
     */
    private static final double THRESHOLD = 5.0001;

    private boolean m_cachedCloneBool;

    /**
     * The horizontal rows being snapped to.
     */
    private Entry[] m_rows;

    /**
     * The vertical columnd being snapped to.
     */
    private Entry[] m_cols;

    /**
     * The y port values of the dragged node.
     */
    private Entry[] m_yValues;

    /**
     * The container editpart providing the coordinates and the children to
     * which snapping occurs.
     */
    private GraphicalEditPart m_container;

    private final ZoomManager m_zoomManager;

    /**
     * Constructs a helper that will use the given part as its basis for
     * snapping. The part's contents pane will provide the coordinate system and
     * its children determine the existing elements.
     *
     * @since 3.0
     * @param container the container editpart
     */
    public SnapToPortGeometry(final GraphicalEditPart container) {
        this.m_container = container;

        m_zoomManager = (ZoomManager)container.getRoot().getViewer()
                .getProperty(ZoomManager.class.toString());
    }

    /**
     * Generates a list of parts which should be snapped to. The list is the
     * original children, minus the given exclusions, minus and children whose
     * figures are not visible.
     *
     * @since 3.0
     * @param exclusions the children to exclude
     * @return a list of parts which should be snapped to
     */
    protected List generateSnapPartsList(final List exclusions) {
        // Don't snap to any figure that is being dragged
        List<Object> children = new ArrayList<Object>(m_container.getChildren());
        children.removeAll(exclusions);

        // Don't snap to hidden figures
        List hiddenChildren = new ArrayList();
        for (Iterator iter = children.iterator(); iter.hasNext();) {
            GraphicalEditPart child = (GraphicalEditPart)iter.next();
            if (!child.getFigure().isVisible()) {
                hiddenChildren.add(child);
            }
        }
        children.removeAll(hiddenChildren);

        return children;
    }

    /**
     * Returns the correction value for the given entries and sides. During a
     * move, the left, right, or center is free to snap to a location.
     *
     * @param entries the entries
     * @param extendedData the requests extended data
     * @param vert <code>true</code> if the correction is vertical
     * @param near the left/top side of the rectangle
     * @param far the right/bottom side of the rectangle
     * @return the correction amount or THRESHOLD if no correction was made
     */
    protected double getCorrectionFor(final Entry[] entries,
            final Map<String, Integer> extendedData, final boolean vert,
            final double near, double far) {
        far -= 1.0;
        double total = near + far;
        // If the width is even (i.e., odd right now because we have reduced one
        // pixel from
        // far) there is no middle pixel so favor the left-most/top-most pixel
        // (which is what
        // populateRowsAndCols() does by using int precision).
        if ((int)(near - far) % 2 != 0) {
            total -= 1.0;
        }
        double result = getCorrectionFor(entries, extendedData, vert,
                total / 2, 0);
        if (result == THRESHOLD) {
            result = getCorrectionFor(entries, extendedData, vert, near, -1);
        }
        if (result == THRESHOLD) {
            result = getCorrectionFor(entries, extendedData, vert, far, 1);
        }
        return result;
    }

    /**
     * Returns the correction value for the given entries and sides. During a
     * move, the left, right, or center is free to snap to a location.
     *
     * @param entries the entries
     * @param extendedData the requests extended data
     * @return the correction amount or THRESHOLD if no correction was made
     */
    protected double getCorrectionForY(final Entry[] entries,
            final Map extendedData, final Entry[] ys, final int moveDelta) {

        // get the smallest distance to the next y value
        double result = Double.MAX_VALUE;
        for (Entry entry : entries) {
            for (Entry y : ys) {

                // only compare inports to outports as only oposite parts
                // can connect and must be alligned
                if (!(entry.m_inport ^ y.m_inport)) {
                    continue;
                }

                // and only ports of same type (data - data, model-model)
                // are snaped
                if (!entry.m_portType .equals(y.m_portType)) {
                    continue;
                }

                double diff = entry.m_offset - (y.m_offset + moveDelta);
                if (Math.abs(diff) < Math.abs(result)) {
                    result = diff;
                }
            }
        }

        return Math.round(result);
    }

    /**
     * Returns the correction value between {@link #THRESHOLD}, or the
     * THRESHOLD if no corrections were found.
     *
     * @param entries the entries
     * @param extendedData the map for setting values
     * @param vert <code>true</code> if vertical
     * @param value the value being corrected
     * @param side which sides should be considered
     * @return the correction or THRESHOLD if no correction was made
     */
    protected double getCorrectionFor(final Entry[] entries,
            final Map<String, Integer> extendedData, final boolean vert,
            final double value, final int side) {
        double resultMag = THRESHOLD;
        double result = THRESHOLD;

        String property;
        if (side == -1) {
            property = vert ? KEY_WEST_ANCHOR : KEY_NORTH_ANCHOR;
        } else {
            property = vert ? KEY_EAST_ANCHOR : KEY_SOUTH_ANCHOR;
        }

        for (int i = 0; i < entries.length; i++) {
            Entry entry = entries[i];
            double magnitude;

            if (entry.m_side == -1 && side != 0) {
                magnitude = Math.abs(value - entry.m_offset);
                if (magnitude < resultMag) {
                    resultMag = magnitude;
                    result = entry.m_offset - value;
                    extendedData.put(property, entry.m_offset);
                }
            } else if (entry.m_side == 0 && side == 0) {
                magnitude = Math.abs(value - entry.m_offset);
                if (magnitude < resultMag) {
                    resultMag = magnitude;
                    result = entry.m_offset - value;
                    extendedData.put(property, entry.m_offset);
                }
            } else if (entry.m_side == 1 && side != 0) {
                magnitude = Math.abs(value - entry.m_offset);
                if (magnitude < resultMag) {
                    resultMag = magnitude;
                    result = entry.m_offset - value;
                    extendedData.put(property, entry.m_offset);
                }
            }
        }
        return result;
    }

    /**
     * Returns the rectangular contribution for the given editpart. This is the
     * rectangle with which snapping is performed.
     *
     * @since 3.0
     * @param part the child
     * @return the rectangular guide for that part
     */
    protected Rectangle getFigureBounds(final GraphicalEditPart part) {
        IFigure fig = part.getFigure();
        if (fig instanceof HandleBounds) {
            return ((HandleBounds)fig).getHandleBounds();
        }
        return fig.getBounds();
    }

    private List<AbstractPortEditPart> getPorts(final List parts) {
        // add the port edit parts to a list
        List<AbstractPortEditPart> portList
            = new ArrayList<AbstractPortEditPart>();

        if (parts != null) {
            for (Object part : parts) {

                if (part instanceof NodeContainerEditPart) {
                    NodeContainerEditPart containerEditPart
                        = (NodeContainerEditPart)part;

                    // get the port parts
                    for (Object childPart : containerEditPart.getChildren()) {
                        if (childPart instanceof AbstractPortEditPart) {
                            // add to list
                            portList.add((AbstractPortEditPart)childPart);
                        }
                    }
                }
                if (part instanceof AbstractWorkflowPortBarEditPart) {
                    AbstractWorkflowPortBarEditPart containerEditPart
                        = (AbstractWorkflowPortBarEditPart)part;

                    // get the port parts
                    for (Object childPart : containerEditPart.getChildren()) {
                        if (childPart instanceof AbstractPortEditPart) {
                            // add to list
                            portList.add((AbstractPortEditPart)childPart);
                        }
                    }
                }
            }
        }

        return portList;
    }

    /**
     * Updates the cached row and column Entries using the provided parts.
     * Columns are only the center of a node figure while rows are all ports of
     * a node.
     *
     * @param parts a List of EditParts
     */
    protected void populateRowsAndCols(final List parts, final List dragedParts) {

        // add the port edit parts to a list
        List<AbstractPortEditPart> portList = getPorts(parts);

        // create all row relevant points fromt the port list
        List<Entry> rowVector = new ArrayList<Entry>();
        for (int i = 0; i < portList.size(); i++) {
            GraphicalEditPart child = portList.get(i);
            Rectangle bounds = getFigureBounds(child);

            // get information is this is an inport
            boolean inport = false;
            if (portList.get(i) instanceof NodeInPortEditPart
                    || portList.get(i) instanceof WorkflowInPortEditPart) {
                inport = true;
            }

            // get information is this is a model port
            rowVector.add(new Entry(0, bounds.y + (bounds.height - 1) / 2,
                    inport, portList.get(i).getType()));
        }

        // add the port edit parts to a list
        List<AbstractPortEditPart> dargedPortList = getPorts(dragedParts);
        for (int i = 0; i < dargedPortList.size(); i++) {

            // for each port get a possible connection (if connected)
            AbstractPortEditPart portPart = dargedPortList.get(i);

            List sourceConnections = portPart.getSourceConnections();
            for (int j = 0; j < sourceConnections.size(); j++) {
                ConnectionContainerEditPart conPart
                    = (ConnectionContainerEditPart)sourceConnections.get(j);

                Point p = ((Connection)conPart.getFigure()).getPoints()
                        .getPoint(2);

                rowVector.add(new Entry(0, p.y, true, portPart.getType()));
            }

            List targetConnections = portPart.getTargetConnections();
            for (int j = 0; j < targetConnections.size(); j++) {
                ConnectionContainerEditPart conPart
                    = (ConnectionContainerEditPart)targetConnections.get(j);

                PointList pList = ((Connection)conPart.getFigure()).getPoints();
                Point p = pList.getPoint(pList.size() - 3);

                rowVector.add(new Entry(0, p.y, false, portPart.getType()));
            }
        }

        List<Entry> colVector = new ArrayList<Entry>();

        for (int i = 0; i < parts.size(); i++) {
            GraphicalEditPart child = (GraphicalEditPart)parts.get(i);
            Rectangle bounds = getFigureBounds(child);
            colVector.add(new Entry(0, bounds.x + (bounds.width - 1) / 2));
        }

        m_rows = rowVector.toArray(new Entry[rowVector.size()]);
        m_cols = colVector.toArray(new Entry[colVector.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int snapRectangle(final Request request, int snapOrientation,
            PrecisionRectangle baseRect, final PrecisionRectangle result) {
        assert (request instanceof ChangeBoundsRequest) : Messages.SnapToPortGeometry_3 + request.getClass();
        ChangeBoundsRequest changeBoundsRequest = (ChangeBoundsRequest)request;

        baseRect = baseRect.getPreciseCopy();
        makeRelative(m_container.getContentPane(), baseRect);
        PrecisionRectangle correction = new PrecisionRectangle();
        makeRelative(m_container.getContentPane(), correction);

        // Recalculate snapping locations if needed
        boolean isClone = request.getType().equals(RequestConstants.REQ_CLONE);
        List exclusionSet = null;
        if (m_rows == null || m_cols == null || isClone != m_cachedCloneBool) {
            m_cachedCloneBool = isClone;
            exclusionSet = Collections.EMPTY_LIST;
            if (!isClone) {
                exclusionSet = changeBoundsRequest.getEditParts();
            }
            populateRowsAndCols(generateSnapPartsList(exclusionSet),
                    exclusionSet);
        }

        if ((snapOrientation & HORIZONTAL) != 0) {
            double xcorrect = getCorrectionFor(m_cols, changeBoundsRequest.getExtendedData(),
                    true, baseRect.preciseX, baseRect.preciseRight());
            if (xcorrect != THRESHOLD) {
                snapOrientation &= ~HORIZONTAL;
                correction.preciseX += xcorrect;
            }
        }

        // get y values of the draged node part ports
        if (exclusionSet != null) {
            List<AbstractPortEditPart> ports = getPorts(exclusionSet);
            Entry[] yValues = new Entry[ports.size()];
            int i = 0;
            for (AbstractPortEditPart port : ports) {

                boolean inport = false;
                if (port instanceof NodeInPortEditPart
                        || port instanceof WorkflowInPortEditPart) {
                    inport = true;
                }

                yValues[i] = new Entry(0, getFigureBounds(port).getLeft().y,
                        inport, port.getType());
                i++;
            }
            m_yValues = yValues;
        }

        // get the move delta of the orignial location
        Point moveDeltaPoint = changeBoundsRequest.getMoveDelta();
        WorkflowEditor.adaptZoom(m_zoomManager, moveDeltaPoint, false);
        int moveDelta = moveDeltaPoint.y;
        if ((snapOrientation & VERTICAL) != 0) {
            double ycorrect = THRESHOLD;
            ycorrect = getCorrectionForY(m_rows, changeBoundsRequest.getExtendedData(),
                    m_yValues, moveDelta);
            if (Math.abs(ycorrect) < THRESHOLD) {
                snapOrientation &= ~VERTICAL;
                correction.preciseY += ycorrect;
            }
        }

        if ((snapOrientation & EAST) != 0) {
            double rightCorrection = getCorrectionFor(m_cols, request
                    .getExtendedData(), true, baseRect.preciseRight() - 1, 1);
            if (rightCorrection != THRESHOLD) {
                snapOrientation &= ~EAST;
                correction.preciseWidth += rightCorrection;
            }
        }

        if ((snapOrientation & WEST) != 0) {
            double leftCorrection = getCorrectionFor(m_cols, request
                    .getExtendedData(), true, baseRect.preciseX, -1);
            if (leftCorrection != THRESHOLD) {
                snapOrientation &= ~WEST;
                correction.preciseWidth -= leftCorrection;
                correction.preciseX += leftCorrection;
            }
        }

        if ((snapOrientation & SOUTH) != 0) {
            double bottom = getCorrectionFor(m_rows, request.getExtendedData(),
                    false, baseRect.preciseBottom() - 1, 1);
            if (bottom != THRESHOLD) {
                snapOrientation &= ~SOUTH;
                correction.preciseHeight += bottom;
            }
        }

        if ((snapOrientation & NORTH) != 0) {
            double topCorrection = getCorrectionFor(m_rows, request
                    .getExtendedData(), false, baseRect.preciseY, -1);
            if (topCorrection != THRESHOLD) {
                snapOrientation &= ~NORTH;
                correction.preciseHeight -= topCorrection;
                correction.preciseY += topCorrection;
            }
        }

        correction.updateInts();
        makeAbsolute(m_container.getContentPane(), correction);
        result.preciseX += correction.preciseX;
        result.preciseY += correction.preciseY;
        result.preciseWidth += correction.preciseWidth;
        result.preciseHeight += correction.preciseHeight;
        result.updateInts();

        return snapOrientation;
    }
}
