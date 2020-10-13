package life.genny.notes.models;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class QDataNoteMessage extends QDataMessage{

	private static final String DATATYPE_NOTE = Note.class.getSimpleName();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Note[] items;
	private Long total;
	private String status;
	
	/**
	 * @param items
	 * @param total
	 */
	public QDataNoteMessage() {
		super(DATATYPE_NOTE);
	}
	
	public QDataNoteMessage(Note item,NoteStatus status) {
		super(DATATYPE_NOTE);
		List<Note> notes = new ArrayList<Note>();
		notes.add(item);
		this.total = 1L;
		this.items = notes.toArray(new Note[0]);
		this.status = status.toString();
		this.setDelete(status.equals(NoteStatus.DELETED));
		this.setReplace(status.equals(NoteStatus.UPDATED));
		this.setSourceAddress(item.sourceCode);
		List<String> codes = new ArrayList<String>();
		codes.add(item.targetCode);
		this.setTargetCodes(codes);

	}
	
	public QDataNoteMessage(List<Note> items, Long total) {
		this(items,total,NoteStatus.READ);
	}
	
	public QDataNoteMessage(List<Note> items, Long total, NoteStatus status) {
		super(DATATYPE_NOTE);
		if ((items == null) || (items.isEmpty())) {
			items = new ArrayList<Note>();
		}

		this.items = items.toArray(new Note[0]);
		this.total = total;
		this.status = status.toString();
		this.setDelete(status.equals(NoteStatus.DELETED));
		this.setReplace(status.equals(NoteStatus.UPDATED));
		List<String> codes = new ArrayList<String>();
		if (!((items == null) || (items.isEmpty()))) {
			this.setSourceAddress(this.items[0].sourceCode);
			codes.add(this.items[0].targetCode);
		}
		this.setTargetCodes(codes);

	}
	/**
	 * @return the items
	 */
	public Note[] getItems() {
		return items;
	}
	/**
	 * @param items the items to set
	 */
	public void setItems(Note[] items) {
		this.items = items;
	}
	/**
	 * @return the total
	 */
	public Long getTotal() {
		return total;
	}
	/**
	 * @param total the total to set
	 */
	public void setTotal(Long total) {
		this.total = total;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	

	
}
