package life.genny.notes.models;


public enum NoteStatus {
	DELETED {
		public String toString() {
			return "DELETED";
		}
	},
	NEW {
		public String toString() {
			return "NEW";
		}
	},
	UPDATED {
		public String toString() {
			return "UPDATED";
		}
	},
	READ {
		public String toString() {
			return "READ";
		}
	}
}
