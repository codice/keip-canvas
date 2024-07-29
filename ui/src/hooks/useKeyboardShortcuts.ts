import { useEffect } from 'react';
import { useKeyPress } from 'reactflow';
import { useUndoRedo } from '../singletons/store';

export const useKeyboardShortcuts = () => {
 
    const { undo, redo } = useUndoRedo()
    
    const undoKeyPressed = useKeyPress(['Control+z', 'Meta+z'])
    const redoKeyPressed = useKeyPress(['Control+Shift+Z', 'Meta+Shift+Z', 'Meta+y', 'Control+y'])
  
    useEffect(() => {
      if (undoKeyPressed) {
        undo()
        
      } else if (redoKeyPressed) {
        redo()
      }
    }, [undoKeyPressed, redoKeyPressed, redo, undo])

}
