/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.database;
/// M: Debug for StaleDataException @{
import android.util.Log;
import java.lang.Throwable;
import android.os.Build;
/// M: }@

/**
 * A base class for Cursors that store their data in {@link CursorWindow}s.
 * <p>
 * The cursor owns the cursor window it uses.  When the cursor is closed,
 * its window is also closed.  Likewise, when the window used by the cursor is
 * changed, its old window is closed.  This policy of strict ownership ensures
 * that cursor windows are not leaked.
 * </p><p>
 * Subclasses are responsible for filling the cursor window with data during
 * {@link #onMove(int, int)}, allocating a new cursor window if necessary.
 * During {@link #requery()}, the existing cursor window should be cleared and
 * filled with new data.
 * </p><p>
 * If the contents of the cursor change or become invalid, the old window must be closed
 * (because it is owned by the cursor) and set to null.
 * </p>
 */
public abstract class AbstractWindowedCursor extends AbstractCursor {
    /**
     * The cursor window owned by this cursor.
     */
    protected CursorWindow mWindow;

    /// M: Debug for StaleDataException @{
    private Throwable mWindowCloseTrace;
    private static final boolean DEBUG_CLOSE_TRACE = Log.isLoggable("DBStaleDataTrace", Log.VERBOSE);
    private static final boolean DEBUG_IS_ENG_BUILD = "eng".equals(Build.TYPE);
    /// M: }@

    @Override
    public byte[] getBlob(int columnIndex) {
        checkPosition();
        return mWindow.getBlob(mPos, columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
        checkPosition();
        return mWindow.getString(mPos, columnIndex);
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        checkPosition();
        mWindow.copyStringToBuffer(mPos, columnIndex, buffer);
    }

    @Override
    public short getShort(int columnIndex) {
        checkPosition();
        return mWindow.getShort(mPos, columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        checkPosition();
        return mWindow.getInt(mPos, columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        checkPosition();
        return mWindow.getLong(mPos, columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) {
        checkPosition();
        return mWindow.getFloat(mPos, columnIndex);
    }

    @Override
    public double getDouble(int columnIndex) {
        checkPosition();
        return mWindow.getDouble(mPos, columnIndex);
    }

    @Override
    public boolean isNull(int columnIndex) {
        checkPosition();
        return mWindow.getType(mPos, columnIndex) == Cursor.FIELD_TYPE_NULL;
    }

    /**
     * @deprecated Use {@link #getType}
     */
    @Deprecated
    public boolean isBlob(int columnIndex) {
        return getType(columnIndex) == Cursor.FIELD_TYPE_BLOB;
    }

    /**
     * @deprecated Use {@link #getType}
     */
    @Deprecated
    public boolean isString(int columnIndex) {
        return getType(columnIndex) == Cursor.FIELD_TYPE_STRING;
    }

    /**
     * @deprecated Use {@link #getType}
     */
    @Deprecated
    public boolean isLong(int columnIndex) {
        return getType(columnIndex) == Cursor.FIELD_TYPE_INTEGER;
    }

    /**
     * @deprecated Use {@link #getType}
     */
    @Deprecated
    public boolean isFloat(int columnIndex) {
        return getType(columnIndex) == Cursor.FIELD_TYPE_FLOAT;
    }

    @Override
    public int getType(int columnIndex) {
        checkPosition();
        return mWindow.getType(mPos, columnIndex);
    }

    @Override
    protected void checkPosition() {
        super.checkPosition();
        
        if (mWindow == null) {
            /// M: Debug for StaleDataException @{
            if(DEBUG_IS_ENG_BUILD || DEBUG_CLOSE_TRACE) {
                Log.v("CursorOrCursorWindowClosed","CursorWindow close stack trace", mWindowCloseTrace);
            }
            /// M: }@

            throw new StaleDataException("Attempting to access a closed CursorWindow." +
                    "Most probable cause: cursor is deactivated prior to calling this method.");
        }
    }

    @Override
    public CursorWindow getWindow() {
        return mWindow;
    }

    /**
     * Sets a new cursor window for the cursor to use.
     * <p>
     * The cursor takes ownership of the provided cursor window; the cursor window
     * will be closed when the cursor is closed or when the cursor adopts a new
     * cursor window.
     * </p><p>
     * If the cursor previously had a cursor window, then it is closed when the
     * new cursor window is assigned.
     * </p>
     *
     * @param window The new cursor window, typically a remote cursor window.
     */
    public void setWindow(CursorWindow window) {
        if (window != mWindow) {
            closeWindow();
            mWindow = window;
        }
    }

    /**
     * Returns true if the cursor has an associated cursor window.
     *
     * @return True if the cursor has an associated cursor window.
     */
    public boolean hasWindow() {
        return mWindow != null;
    }

    /**
     * Closes the cursor window and sets {@link #mWindow} to null.
     * @hide
     */
    protected void closeWindow() {
        if (mWindow != null) {
            mWindow.close();
            mWindow = null;

            /// M: Debug for StaleDataException @{
            if(DEBUG_IS_ENG_BUILD || DEBUG_CLOSE_TRACE) {
                mWindowCloseTrace = new Throwable("stacktrace");
            }
            /// M: }@
        }
    }

    /**
     * If there is a window, clear it.
     * Otherwise, creates a new window.
     *
     * @param name The window name.
     * @hide
     */
    protected void clearOrCreateWindow(String name) {
        if (mWindow == null) {
            mWindow = new CursorWindow(name);
        } else {
            mWindow.clear();
        }
    }

    /** @hide */
    @Override
    protected void onDeactivateOrClose() {
        super.onDeactivateOrClose();
        closeWindow();
    }
}
