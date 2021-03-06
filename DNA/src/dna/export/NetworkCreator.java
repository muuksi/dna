package dna.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import org.jdom.Attribute;
import org.jdom.Comment;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.Document;

import dna.Dna;
import dna.dataStructures.Statement;

public class NetworkCreator {
	ExportSetting exportSetting;
	
	public NetworkCreator(ExportSetting exportSetting) {
		this.exportSetting = exportSetting;
		ArrayList<Statement> statements = filter();
		if (exportSetting.getNetworkType().equals("eventList")) {
			releventCSV(statements, exportSetting.getFileName());
			JOptionPane.showMessageDialog(Dna.dna.gui, "File has been exported to \"" + exportSetting.getFileName() + "\".");
		} else if (exportSetting.getNetworkType().equals("twoMode")) {
			Network network = twoMode(statements, exportSetting.getVar1(), exportSetting.getVar2(), exportSetting.isCountDuplicates(), 
					exportSetting.isIncludeIsolates());
			if (exportSetting.getExportFormat().equals(".csv")) {
				exportCSV(network, exportSetting.getFileName());
			} else if (exportSetting.getExportFormat().equals(".dl")) {
				exportDL(network, exportSetting.getFileName());
			} else if (exportSetting.getExportFormat().equals(".graphml")) {
				exportGraphML(network, exportSetting.getFileName());
			}
			JOptionPane.showMessageDialog(Dna.dna.gui, "File has been exported to \"" + exportSetting.getFileName() + "\".");
		} else if (exportSetting.getNetworkType().equals("oneMode")){
			Network network = oneMode(statements, exportSetting.getVar1(), exportSetting.getVar2(), exportSetting.getQualifier(), 
					exportSetting.isCountDuplicates(), exportSetting.isIncludeIsolates());
			if (exportSetting.getExportFormat().equals(".csv")) {
				exportCSV(network, exportSetting.getFileName());
			} else if (exportSetting.getExportFormat().equals(".dl")) {
				exportDL(network, exportSetting.getFileName());
			} else if (exportSetting.getExportFormat().equals(".graphml")) {
				exportGraphML(network, exportSetting.getFileName());
			}
			JOptionPane.showMessageDialog(Dna.dna.gui, "File has been exported to \"" + exportSetting.getFileName() + "\".");
		} else {
			System.err.println("Unknown network type: " + exportSetting.getNetworkType());
		}
	}
	
	/**
	 * Return a filtered list of {@link SidebarStatement}s based on the settings saved in the {@link NetworkExporterObject}.
	 * 
	 * @return	ArrayList of filtered {@link SidebarStatement}s
	 */
	ArrayList<Statement> filter() {
		ArrayList<Statement> al = new ArrayList<Statement>();
		for (int i = 0; i < Dna.dna.gui.rightPanel.statementPanel.ssc.size(); i++) {
			boolean select = true;
			Statement s = Dna.dna.gui.rightPanel.statementPanel.ssc.get(i);
			
			// step 1: get all statement IDs corresponding to date range and statement type
			if (s.getDate().before(exportSetting.getStartDate())) {
				select = false;
			} else if (s.getDate().after(exportSetting.getStopDate())) {
				select = false;
			} else if (s.getStatementTypeId() != exportSetting.getStatementType().getId()) {
				select = false;
			}
			
			// step 2: check against excluded values
			if (exportSetting.getAuthorExclude().contains(Dna.data.getDocument(s.getDocumentId()).getAuthor())) {
				select = false;
			} else if (exportSetting.getSourceExclude().contains(Dna.data.getDocument(s.getDocumentId()).getSource())) {
				select = false;
			} else if (exportSetting.getSectionExclude().contains(Dna.data.getDocument(s.getDocumentId()).getSection())) {
				select = false;
			} else if (exportSetting.getTypeExclude().contains(Dna.data.getDocument(s.getDocumentId()).getType())) {
				select = false;
			}
			Iterator<String> keyIterator = exportSetting.getExcludeValues().keySet().iterator();
			while (keyIterator.hasNext()) {
				String key = keyIterator.next();
				String string = "";
				if (exportSetting.getStatementType().getVariables().get(key).equals("boolean") || exportSetting.getStatementType().getVariables().get(key).equals("integer")) {
					string = String.valueOf(s.getValues().get(key));
				} else {
					string = (String) s.getValues().get(key);
				}
				if (exportSetting.getExcludeValues().get(key).contains(string)) {
					select = false;
				}
			}
			
			// step 3: check against statement type
			if (s.getStatementTypeId() != exportSetting.getStatementType().getId()) {
				select = false;
			}
			
			if (select == true) {
				al.add(s);
			}
		}
		return(al);
	}
	
	/**
	 * Compute the matrix product of two two-dimensional arrays.
	 * 
	 * @param mat1	Two-dimensional array with the first input matrix.
	 * @param mat2	Two-dimensional array with the second input matrix.
	 * @return		Two-dimensional array with the output matrix.
	 */
	public static double[][] multiply(double[][] mat1, double[][] mat2) {
        int aRows = mat1.length;
        int aColumns = mat1[0].length;
        int bRows = mat2.length;
        int bColumns = mat2[0].length;
        
        if (aColumns != bRows) {
            throw new IllegalArgumentException("A:Rows: " + aColumns + " did not match B:Columns " + bRows + ".");
        }
        
        double[][] mat3 = new double[aRows][bColumns];
        
        for (int i = 0; i < aRows; i++) { // aRow
            for (int j = 0; j < bColumns; j++) { // bColumn
                for (int k = 0; k < aColumns; k++) { // aColumn
                    mat3[i][j] += mat1[i][k] * mat2[k][j];
                }
            }
        }
        
        return mat3;
    }
	
	/**
	 * Transpose a two-dimensional array.
	 * 
	 * @param mat	Two-dimensional array that should be transposed.
	 * @return		Transposed two-dimensional array.
	 */
	public static double[][] transpose(double[][] mat) {
	    int m = mat.length;
	    int n = mat[0].length;

	    double[][] t = new double[n][m];

	    for(int i = 0; i < n; i++) {
	    	for(int j = 0; j < m; j++) {
	            t[i][j] = mat[j][i];
	        }
	    }
	    return t;
	}

	/**
	 * Add two two-dimensional arrays.
	 * 
	 * @param mat1	Two-dimensional array with the first input matrix.
	 * @param mat2	Two-dimensional array with the second input matrix.
	 * @return		Two-dimensional array with the output matrix.
	 */
	public static double[][] add(double[][] mat1, double[][] mat2) {
        int aRows = mat1.length;
        int aColumns = mat1[0].length;
        int bRows = mat2.length;
        int bColumns = mat2[0].length;

        if (aRows != bRows) {
            throw new IllegalArgumentException("Matrix dimensions do not match: " + aRows + " vs. " + bRows + " rows.");
        }
        if (aColumns != bColumns) {
            throw new IllegalArgumentException("Matrix dimensions do not match: " + aColumns + " vs. " + bColumns + " columns.");
        }
        
        double[][] mat3 = new double[aRows][aColumns];
        
        for (int i = 0; i < aRows; i++) {
            for (int j = 0; j < aColumns; j++) {
                mat3[i][j] = mat1[i][j] + mat2[i][j];
            }
        }
        
        return mat3;
    }

	/**
	 * Subtract second from first two-dimensional array.
	 * 
	 * @param mat1	Two-dimensional array with the first input matrix.
	 * @param mat2	Two-dimensional array with the second input matrix.
	 * @return		Two-dimensional array with the output matrix.
	 */
	public static double[][] subtract(double[][] mat1, double[][] mat2) {
        int aRows = mat1.length;
        int aColumns = mat1[0].length;
        int bRows = mat2.length;
        int bColumns = mat2[0].length;

        if (aRows != bRows) {
            throw new IllegalArgumentException("Matrix dimensions do not match: " + aRows + " vs. " + bRows + " rows.");
        }
        if (aColumns != bColumns) {
            throw new IllegalArgumentException("Matrix dimensions do not match: " + aColumns + " vs. " + bColumns + " columns.");
        }
        
        double[][] mat3 = new double[aRows][aColumns];
        
        for (int i = 0; i < aRows; i++) {
            for (int j = 0; j < aColumns; j++) {
                mat3[i][j] = mat1[i][j] - mat2[i][j];
            }
        }
        
        return mat3;
    }
	
	public Network twoMode(ArrayList<Statement> statements, String var1, String var2, boolean countDuplicates, boolean includeIsolates) {
		
		// step 1: get vectors with unique labels for rows and columns
		ArrayList<String> names1 = new ArrayList<String>(); // unique row labels
		ArrayList<String> names2 = new ArrayList<String>(); // unique column labels
		if (includeIsolates == true) {  // take them from the main database 
			for (int i = 0; i < Dna.data.getStatements().size(); i++) {
				if (Dna.data.getStatements().get(i).getStatementTypeId() == exportSetting.getStatementType().getId()) {
					String n1 = (String) Dna.data.getStatements().get(i).getValues().get(var1);
					String n2 = (String) Dna.data.getStatements().get(i).getValues().get(var2);
					if (!names1.contains(n1)) {
						names1.add(n1);
					}
					if (!names2.contains(n2)) {
						names2.add(n2);
					}
				}
			}
		} else {  // no isolates: take them from the filtered results
			for (int i = 0; i < statements.size(); i++) {
				String n1 = (String) statements.get(i).getValues().get(var1);
				String n2 = (String) statements.get(i).getValues().get(var2);
				if (!names1.contains(n1)) {
					names1.add(n1);
				}
				if (!names2.contains(n2)) {
					names2.add(n2);
				}
			}
		}
		Collections.sort(names1);
		Collections.sort(names2);
		/*
		for (int i = 0; i < names1.size(); i++) {
			System.out.println(i + " " + names1.get(i));
		}
		for (int i = 0; i < names2.size(); i++) {
			System.out.println(i + " " + names2.get(i));
		}
		*/
		
		// step 2: create and populate matrix
		double[][] mat = new double[names1.size()][names2.size()]; // the resulting affiliation matrix; 0 by default
		for (int i = 0; i < statements.size(); i++) {
			String n1 = (String) statements.get(i).getValues().get(var1);
			String n2 = (String) statements.get(i).getValues().get(var2);
			
			int row = -1;
			for (int j = 0; j < names1.size(); j++) {
				if (names1.get(j).equals(n1)) {
					row = j;
					break;
				}
			}
			int col = -1;
			for (int j = 0; j < names2.size(); j++) {
				if (names2.get(j).equals(n2)) {
					col = j;
					break;
				}
			}
			
			if (countDuplicates == true) {
				mat[row][col] = mat[row][col] + 1.0;
			} else {
				boolean duplicate = false;
				if (i > 1) {
					for (int j = i - 1; j > -1; j--) {  // go back to previous statements and check if duplicate; add if no duplicate
						if (statements.get(j).getDocumentId() == statements.get(i).getDocumentId() && 
								statements.get(j).getValues().get(var1).equals(statements.get(i).getValues().get(var1)) && 
								statements.get(j).getValues().get(var2).equals(statements.get(i).getValues().get(var2))) {
							duplicate = true;
							break;
						}
					}
				}
				if (duplicate == false) {
					mat[row][col] = mat[row][col] + 1.0;
				}
			}
		}
		
		// step 3: create Network object and return
		String[] rownames = new String[names1.size()]; // cast row names from array list to array
		rownames = names1.toArray(rownames);
		String[] colnames = new String[names2.size()]; // cast column names from array list to array
		colnames = names2.toArray(colnames);
		Matrix matrix = new Matrix(mat, rownames, colnames); // assemble the Matrix object with labels
		Network network = new Network(matrix, 2);  // wrap matrix in a network object
		return network;
	}
	
	public Network oneMode(ArrayList<Statement> statements, String var1, String var2, String qualifier, boolean countDuplicates, 
			boolean includeIsolates) {
		
		// step 1: collect values of the qualifier variable
		String[] qualifierValues = Dna.data.getStringEntries(exportSetting.getStatementType().getId(), qualifier);
		boolean qualifierInt;
		if (exportSetting.getStatementType().getVariables().get(qualifier).equals("long text") || 
				exportSetting.getStatementType().getVariables().get(qualifier).equals("short text")) {
			qualifierInt = false;
		} else {
			qualifierInt = true;
		}
		
		// step 2: get vectors with unique labels for rows and columns of a two-mode matrix
		ArrayList<String> names1 = new ArrayList<String>(); // unique row labels
		ArrayList<String> names2 = new ArrayList<String>(); // unique column labels
		if (includeIsolates == true) {  // take them from the main database 
			for (int i = 0; i < Dna.data.getStatements().size(); i++) {
				String n1 = (String) Dna.data.getStatements().get(i).getValues().get(var1);
				String n2 = (String) Dna.data.getStatements().get(i).getValues().get(var2);
				if (!names1.contains(n1)) {
					names1.add(n1);
				}
				if (!names2.contains(n2)) {
					names2.add(n2);
				}
			}
		} else {  // no isolates: take them from the filtered results
			for (int i = 0; i < statements.size(); i++) {
				String n1 = (String) statements.get(i).getValues().get(var1);
				String n2 = (String) statements.get(i).getValues().get(var2);
				if (!names1.contains(n1)) {
					names1.add(n1);
				}
				if (!names2.contains(n2)) {
					names2.add(n2);
				}
			}
		}
		Collections.sort(names1);
		Collections.sort(names2);
		
		// step 3: create congruence network; go through each qualifier level, create two-mode network, project one-mode network, and add
		double[][] congruence = new double[names1.size()][names1.size()];  // quadratic matrix!
		if (exportSetting.getAgreementPattern().equals("congruence") || exportSetting.getAgreementPattern().equals("subtract")) {
			for (int i = 0; i < qualifierValues.length; i++) {
				
				// only retain statements with this qualifier level
				ArrayList<Statement> al = new ArrayList<Statement>();
				for (int j = 0; j < statements.size(); j++) {
					String currentQualifier = "";
					if (qualifierInt == true) {
						currentQualifier = String.valueOf(statements.get(j).getValues().get(qualifier));
					} else {
						currentQualifier = (String) statements.get(j).getValues().get(qualifier);
					}
					if (qualifierValues[i].equals(currentQualifier)) {
						al.add(statements.get(j));
					}
				}
				
				// create two-mode network at this qualifier level
				double[][] mat = new double[names1.size()][names2.size()];  // rectangular matrix!
				for (int k = 0; k < statements.size(); k++) {
					String n1 = (String) statements.get(k).getValues().get(var1);
					String n2 = (String) statements.get(k).getValues().get(var2);
					
					int row = -1;
					for (int l = 0; l < names1.size(); l++) {
						if (names1.get(l).equals(n1)) {
							row = l;
							break;
						}
					}
					int col = -1;
					for (int l = 0; l < names2.size(); l++) {
						if (names2.get(l).equals(n2)) {
							col = l;
							break;
						}
					}
					
					if (countDuplicates == true) {
						mat[row][col] = mat[row][col] + 1.0;
					} else {
						boolean duplicate = false;
						if (k > 1) {
							for (int l = k - 1; l > -1; l--) {  // go back to previous statements and check if duplicate; add if no duplicate
								if (statements.get(l).getDocumentId() == statements.get(k).getDocumentId() && 
										statements.get(l).getValues().get(var1).equals(statements.get(k).getValues().get(var1)) && 
										statements.get(l).getValues().get(var2).equals(statements.get(k).getValues().get(var2))) {
									duplicate = true;
									break;
								}
							}
						}
						if (duplicate == false) {
							mat[row][col] = mat[row][col] + 1.0;
						}
					}
				}
				
				// convert two-mode into one-mode network and add to congruence matrix
				mat = multiply(mat, transpose(mat));
				congruence = add(congruence, mat);
			}
		}
		
		// step 4: create conflict network if necessary
		double[][] conflict = new double[names1.size()][names1.size()];  // quadratic matrix!
		if (exportSetting.getAgreementPattern().equals("conflict") || exportSetting.getAgreementPattern().equals("subtract")) {
			for (int i = 0; i < names1.size(); i++) {
				for (int j = 0; j < names1.size(); j++) {
					for (int k = 0; k < statements.size(); k++) {
						for (int l = 0; l < statements.size(); l++) {
							if (i != j && names1.get(i).equals(statements.get(k).getValues().get(var1)) && 
									names1.get(j).equals(statements.get(l).getValues().get(var1))) {
								if (qualifierInt == false) {
									if (!statements.get(k).getValues().get(qualifier).equals(statements.get(l).getValues().get(qualifier))) {
										if (countDuplicates == true) {
											conflict[i][j] = conflict[i][j] + 1;
										} else {
											boolean duplicate = false;
											if (k > 1) {
												for (int m = k - 1; m > -1; m--) {  // go back to previous statements and check if duplicate; add if no duplicate
													if (statements.get(m).getDocumentId() == statements.get(k).getDocumentId() && 
															statements.get(m).getValues().get(var1).equals(statements.get(k).getValues().get(var1)) && 
															statements.get(m).getValues().get(var2).equals(statements.get(k).getValues().get(var2)) && 
															statements.get(m).getValues().get(qualifier).equals(statements.get(k).getValues().get(qualifier))) {
														duplicate = true;
														break;
													}
												}
											}
											if (duplicate == false) {
												conflict[i][j] = conflict[i][j] + 1;
											}
										}
									}
								} else {
									int q1 = (int) statements.get(k).getValues().get(qualifier);
									int q2 = (int) statements.get(l).getValues().get(qualifier);
									int difference = Math.abs(q1 - q2);
									
									if (countDuplicates == true) {
										conflict[i][j] = conflict[i][j] + difference;
									} else {
										boolean duplicate = false;
										if (k > 1) {
											for (int m = k - 1; m > -1; m--) {  // go back to previous statements and check if duplicate; add if no duplicate
												if (statements.get(m).getDocumentId() == statements.get(k).getDocumentId() && 
														statements.get(m).getValues().get(var1).equals(statements.get(k).getValues().get(var1)) && 
														statements.get(m).getValues().get(var2).equals(statements.get(k).getValues().get(var2)) && 
														statements.get(m).getValues().get(qualifier) == statements.get(k).getValues().get(qualifier)) {
													duplicate = true;
													break;
												}
											}
										}
										if (duplicate == false) {
											conflict[i][j] = conflict[i][j] + difference;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		// step 5: create ignore matrix if necessary
		double[][] ignore = new double[names1.size()][names2.size()];  // rectangular matrix!
		if (exportSetting.getAgreementPattern().equals("ignore")) {
			for (int k = 0; k < statements.size(); k++) {
				String n1 = (String) statements.get(k).getValues().get(var1);
				String n2 = (String) statements.get(k).getValues().get(var2);
				
				int row = -1;
				for (int l = 0; l < names1.size(); l++) {
					if (names1.get(l).equals(n1)) {
						row = l;
						break;
					}
				}
				int col = -1;
				for (int l = 0; l < names2.size(); l++) {
					if (names2.get(l).equals(n2)) {
						col = l;
						break;
					}
				}
				
				if (countDuplicates == true) {
					ignore[row][col] = ignore[row][col] + 1.0;
				} else {
					boolean duplicate = false;
					if (k > 1) {
						for (int l = k - 1; l > -1; l--) {  // go back to previous statements and check if duplicate; add if no duplicate
							if (statements.get(l).getDocumentId() == statements.get(k).getDocumentId() && 
									statements.get(l).getValues().get(var1).equals(statements.get(k).getValues().get(var1)) && 
									statements.get(l).getValues().get(var2).equals(statements.get(k).getValues().get(var2))) {
								duplicate = true;
								break;
							}
						}
					}
					if (duplicate == false) {
						ignore[row][col] = ignore[row][col] + 1.0;
					}
				}
			}
			ignore = multiply(ignore, transpose(ignore));  // convert two-mode into one-mode network
		}
		
		// step 5: choose result matrix; subtract if necessary
		double[][] resultmat = new double[names1.size()][names1.size()];  // quadratic matrix!
		if (exportSetting.getAgreementPattern().equals("congruence")) {
			resultmat = congruence;
		} else if (exportSetting.getAgreementPattern().equals("conflict")) {
			resultmat = conflict;
		} else if (exportSetting.getAgreementPattern().equals("ignore")) {
			resultmat = ignore;
		} else if (exportSetting.getAgreementPattern().equals("subtract")) {
			resultmat = subtract(congruence, conflict);
		}
		
		// step 6: normalization
		if (exportSetting.getNormalization().equals("cooccurrence")) {
			// do nothing
		} else if (exportSetting.getNormalization().equals("average")) {
			Network tm = twoMode(statements, var1, var2, countDuplicates, includeIsolates);
			double[][] mat = tm.getMatrix().getMatrix();
			int columns = tm.getMatrix().getColnames().length;
			if (tm.getMatrix().getRownames().length != names1.size()) {
				System.err.println("Normalization: dimensions differ. Network is not normalized.");
			} else {
				for (int i = 0; i < names1.size(); i++) {
					for (int j = 0; j < names1.size(); j++) {
						int activity_i = 0;
						int activity_j = 0;
						for (int k = 0; k < columns; k++) {
							if (mat[i][k] > 0) {
								activity_i++;
							}
							if (mat[j][k] > 0) {
								activity_j++;
							}
						}
						resultmat[i][j] = resultmat[i][j] / (activity_i + activity_j) / 2;
					}
				}
			}
		} else if (exportSetting.getNormalization().equals("jaccard")) {
			System.out.println("Warning: Jaccard normalization is experimental and may return wrong results.");
			Network tm = twoMode(statements, var1, var2, countDuplicates, includeIsolates);
			double[][] mat = tm.getMatrix().getMatrix();
			int columns = tm.getMatrix().getColnames().length;
			if (tm.getMatrix().getRownames().length != names1.size()) {
				System.err.println("Normalization: dimensions differ. Network is not normalized.");
			} else {
				for (int i = 0; i < names1.size(); i++) {
					for (int j = 0; j < names1.size(); j++) {
						int m01 = 0;
						int m10 = 0;
						int m11 = 0;
						for (int k = 0; k < columns; k++) {
							if (mat[i][k] > 0 && mat[j][k] > 0) {
								m11++;
							} else if (mat[i][k] > 0) {
								m10++;
							} else if (mat[j][k] > 0) {
								m01++;
							}
						}
						resultmat[i][j] = resultmat[i][j] / (m01 + m10 + m11);
					}
				}
			}
		} else if (exportSetting.getNormalization().equals("cosine")) {
			Network tm = twoMode(statements, var1, var2, countDuplicates, includeIsolates);
			double[][] mat = tm.getMatrix().getMatrix();
			int columns = tm.getMatrix().getColnames().length;
			if (tm.getMatrix().getRownames().length != names1.size()) {
				System.err.println("Normalization: dimensions differ. Network is not normalized.");
			} else {
				for (int i = 0; i < names1.size(); i++) {
					for (int j = 0; j < names1.size(); j++) {
						int activity_i = 0;
						int activity_j = 0;
						for (int k = 0; k < columns; k++) {
							if (mat[i][k] > 0) {
								activity_i++;
							}
							if (mat[j][k] > 0) {
								activity_j++;
							}
						}
						resultmat[i][j] = resultmat[i][j] / Math.sqrt(activity_i * activity_j);
					}
				}
			}
		} else {
			System.err.println("Normalization argument not recognized.");
		}
		
		// step 7: create Network object and return
		String[] rownames = new String[names1.size()]; // cast row/column names from array list to array
		rownames = names1.toArray(rownames);
		Matrix matrix = new Matrix(resultmat, rownames, rownames); // assemble the Matrix object with labels
		Network network = new Network(matrix, 1);  // wrap matrix in a network object
		return network;
	}
	
	/**
	 * This function accepts a list of statements that should be included in the relational event export, 
	 * and it exports the variables of all statements to a CSV file, along with the statement ID and a 
	 * date/time stamp. There is one statement per row, and the number of columns is the number of variables 
	 * present in the statement type.
	 * 
	 * @param statements	An array list of SidebarStatement objects (of the same statement type) that should be exported.
	 * @param fileName		String with the file name of the CSV file to which the event list will be exported.
	 */
	public void releventCSV(ArrayList<Statement> statements, String fileName) {
		String key, value;
		int statementId;
		Date d;
		SimpleDateFormat dateFormat;
		int statementTypeId = statements.get(0).getStatementTypeId();
		for (int i = 0; i < statements.size(); i++) {
			if (statements.get(i).getStatementTypeId() != statementTypeId) {
				throw new IllegalArgumentException("More than one statement type was selected. Cannot export to a spreadsheet!");
			}
		}
		HashMap<String, String> variables = Dna.data.getStatementTypeById(statementTypeId).getVariables();
		Iterator<String> keyIterator;
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
			keyIterator = variables.keySet().iterator();
			out.write("\"statement ID\";\"time\";\"document ID\";\"document title\";\"author\";\"source\";\"section\";\"type\";\"text\"");
			while (keyIterator.hasNext()){
				out.write(";\"" + keyIterator.next() + "\"");
			}
			for (int i = 0; i < statements.size(); i++) {
				out.newLine();
				statementId = statements.get(i).getId();
				String stringId = new Integer(statementId).toString();
				out.write(stringId);
				d = statements.get(i).getDate();
				dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				out.write(";" + dateFormat.format(d));
				dna.dataStructures.Document doc = Dna.data.getDocument(Dna.data.getStatement(statementId).getDocumentId());
				out.write(";" + doc.getId());
				out.write(";\"" + doc.getTitle().replaceAll(";", ",").replaceAll("\"", "'") + "\"");
				out.write(";\"" + doc.getAuthor().replaceAll(";", ",").replaceAll("\"", "'") + "\"");
				out.write(";\"" + doc.getSource().replaceAll(";", ",").replaceAll("\"", "'") + "\"");
				out.write(";\"" + doc.getSection().replaceAll(";", ",").replaceAll("\"", "'") + "\"");
				out.write(";\"" + doc.getType().replaceAll(";", ",").replaceAll("\"", "'") + "\"");
				out.write(";\"" + doc.getText().substring(Dna.data.getStatement(statementId).getStart(), 
						Dna.data.getStatement(statementId).getStop()).replaceAll(";", ",").replaceAll("\"", "'") + "\"");
				keyIterator = variables.keySet().iterator();
				while (keyIterator.hasNext()){
					key = keyIterator.next();
					value = variables.get(key);
					if (value.equals("short text") || value.equals("long text")) {
						out.write(";\"" + ((String) Dna.data.getStatement(statementId).getValues().get(key)).replaceAll(";", ",").replaceAll("\"", "'") + "\"");
					} else if (value.equals("boolean") || value.equals("integer")) {
						out.write(";" + Dna.data.getStatement(statementId).getValues().get(key));
					}
				}
			}
			out.close();
			System.out.println("File has been exported to \"" + fileName + "\".");
		} catch (IOException e) {
			System.err.println("Error while saving CSV file: " + e);
		}
	}

	/**
	 * Export network to a CSV matrix file.
	 * 
	 * @param network  The input Network object.
	 * @param outfile  The path and file name of the target CSV file.
	 */
	public void exportCSV (Network network, String outfile) {
		int nr = network.getMatrix().rownames.length;
		int nc = network.getMatrix().colnames.length;
		String[] rn = network.getMatrix().rownames;
		String[] cn = network.getMatrix().colnames;
		double[][] mat = network.getMatrix().getMatrix();
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile), "UTF8"));
			out.write("\"\"");
			for (int i = 0; i < nc; i++) {
				out.write(";\"" + cn[i] + "\"");
			}
			for (int i = 0; i < nr; i++) {
				out.newLine();
				out.write("\"" + rn[i] + "\"");
				for (int j = 0; j < nc; j++) {
					out.write(";" + String.format(new Locale("en"), "%.6f", mat[i][j]));
				}
			}
			out.close();
		} catch (IOException e) {
			System.err.println("Error while saving CSV matrix file.");
		}
	}

	/**
	 * Export network to a DL fullmatrix file for the software Ucinet.
	 * 
	 * @param network  The input Network object.
	 * @param outfile  The path and file name of the target .dl file.
	 */
	public void exportDL (Network network, String outfile) {
		int nr = network.getMatrix().rownames.length;
		int nc = network.getMatrix().colnames.length;
		String[] rn = network.getMatrix().rownames;
		String[] cn = network.getMatrix().colnames;
		double[][] mat = network.getMatrix().getMatrix();
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile), "UTF8"));
			out.write("DL");
			out.newLine();
			if (network.getModes() == 1) {
				out.write("N=" + nr);
			} else if (network.getModes() == 2) {
				out.write("NR=" + nr + ", NC=" + nc);
			}
			out.newLine();
			out.write("FORMAT = FULLMATRIX DIAGONAL PRESENT");
			out.newLine();
			out.write("ROW LABELS:");
			for (int i = 0; i < nr; i++) {
				out.newLine();
				out.write("\"" + rn[i] + "\"");
			}
			out.newLine();
			out.write("ROW LABELS EMBEDDED");
			out.newLine();
			out.write("COLUMN LABELS:");
			for (int i = 0; i < nc; i++) {
				out.newLine();
				out.write("\"" + cn[i] + "\"");
			}
			out.newLine();
			out.write("COLUMN LABELS EMBEDDED");
			out.newLine();
			out.write("DATA:");
			for (int i = 0; i < nr; i++) {
				out.newLine();
				for (int j = 0; j < nc; j++) {
					out.write(" " + String.format(new Locale("en"), "%.6f", mat[i][j]));
				}
			}
			out.close();
		} catch (IOException e) {
			System.err.println("Error while saving DL fullmatrix file.");
		}
	}

	/**
	 * Export filter for graphML files.
	 * 
	 * @param network  The input Network object.
	 * @param outfile  The path and file name of the target .graphML file.
	 */
	private void exportGraphML(Network network, String outfile) {
		ArrayList<Edge> edges = network.getEdgelist().getEdgelist();
		int modes = network.getModes();
		String[] rn = network.getEdgelist().getSources();
		String[] cn = network.getEdgelist().getTargets();
		int nr = rn.length;
		int nc = cn.length;
		int numVertices = nr;
		if (modes == 1) {
			numVertices = nr;
		} else if (modes == 2) {
			numVertices = nr + nc;
		}
		int numEdges = edges.size();
		String[] labels = null;
		if (modes == 1) {
			labels = rn;
		} else if (modes == 2) {
			labels = Stream.concat(Arrays.stream(rn), Arrays.stream(cn)).toArray(String[]::new);
		}
		
		Namespace xmlns = Namespace.getNamespace("http://graphml.graphdrawing.org/xmlns");
		Element graphml = new Element("graphml", xmlns);
		Namespace visone = Namespace.getNamespace("visone", "http://visone.info/xmlns");
		graphml.addNamespaceDeclaration(visone);
		Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		graphml.addNamespaceDeclaration(xsi);
		Namespace yNs = Namespace.getNamespace("y", "http://www.yworks.com/xml/graphml");
		graphml.addNamespaceDeclaration(yNs);
		Attribute attSchema = new Attribute("schemaLocation", "http://graphml.graphdrawing.org/xmlns/graphml http://www.yworks.com/xml/schema/graphml/1.0/ygraphml.xsd ", xsi);
		graphml.setAttribute(attSchema);
		Document document = new org.jdom.Document(graphml);
		
		Comment dataSchema = new Comment(" data schema ");
		graphml.addContent(dataSchema);
		
		Element keyVisoneNode = new Element("key", xmlns);
		keyVisoneNode.setAttribute(new Attribute("for", "node"));
		keyVisoneNode.setAttribute(new Attribute("id", "d0"));
		keyVisoneNode.setAttribute(new Attribute("yfiles.type", "nodegraphics"));
		graphml.addContent(keyVisoneNode);

		Element keyVisoneEdge = new Element("key", xmlns);
		keyVisoneEdge.setAttribute(new Attribute("for", "edge"));
		keyVisoneEdge.setAttribute(new Attribute("id", "e0"));
		keyVisoneEdge.setAttribute(new Attribute("yfiles.type", "edgegraphics"));
		graphml.addContent(keyVisoneEdge);

		Element keyVisoneGraph = new Element("key", xmlns);
		keyVisoneGraph.setAttribute(new Attribute("for", "graph"));
		keyVisoneGraph.setAttribute(new Attribute("id", "prop"));
		keyVisoneGraph.setAttribute(new Attribute("visone.type", "properties"));
		graphml.addContent(keyVisoneGraph);
		
		Element keyId = new Element("key", xmlns);
		keyId.setAttribute(new Attribute("id", "id"));
		keyId.setAttribute(new Attribute("for", "node"));
		keyId.setAttribute(new Attribute("attr.name", "id"));
		keyId.setAttribute(new Attribute("attr.type", "string"));
		graphml.addContent(keyId);
		
		Element keyClass = new Element("key", xmlns);
		keyClass.setAttribute(new Attribute("id", "class"));
		keyClass.setAttribute(new Attribute("for", "node"));
		keyClass.setAttribute(new Attribute("attr.name", "class"));
		keyClass.setAttribute(new Attribute("attr.type", "string"));
		graphml.addContent(keyClass);
		
		/*
		Element keyFrequency = new Element("key", xmlns);
		keyFrequency.setAttribute(new Attribute("id", "statementFrequency"));
		keyFrequency.setAttribute(new Attribute("for", "node"));
		keyFrequency.setAttribute(new Attribute("attr.name", "statementFrequency"));
		keyFrequency.setAttribute(new Attribute("attr.type", "int"));
		graphml.addContent(keyFrequency);
		*/
		
		Element keyWeight = new Element("key", xmlns);
		keyWeight.setAttribute(new Attribute("id", "weight"));
		keyWeight.setAttribute(new Attribute("for", "edge"));
		keyWeight.setAttribute(new Attribute("attr.name", "weight"));
		keyWeight.setAttribute(new Attribute("attr.type", "double"));
		graphml.addContent(keyWeight);
		
		Element graphElement = new Element("graph", xmlns);
		graphElement.setAttribute(new Attribute("edgedefault", "undirected"));
		
		graphElement.setAttribute(new Attribute("id", "G"));
		graphElement.setAttribute(new Attribute("parse.edges", String.valueOf(numEdges)));
		graphElement.setAttribute(new Attribute("parse.nodes", String.valueOf(numVertices)));
		graphElement.setAttribute(new Attribute("parse.order", "free"));
		Element properties = new Element("data", xmlns);
		properties.setAttribute(new Attribute("key", "prop"));
		Element labelAttribute = new Element("labelAttribute", visone);
		labelAttribute.setAttribute("edgeLabel", "weight");
		labelAttribute.setAttribute("nodeLabel", "id");
		properties.addContent(labelAttribute);
		graphElement.addContent(properties);
		
		Comment nodes = new Comment(" nodes ");
		graphElement.addContent(nodes);
		
		for (int i = 0; i < labels.length; i++) {
			Element node = new Element("node", xmlns);
			node.setAttribute(new Attribute("id", labels[i]));
			
			Element id = new Element("data", xmlns);
			id.setAttribute(new Attribute("key", "id"));
			id.setText(labels[i]);
			node.addContent(id);
			
			String hex;
			String type;
			String shapeString;
			if (i > nr) {
				hex = "#00FF00";  // green
				type = "mode2";
				shapeString = "ellipse";
			} else {
				hex = "#3399FF";  // light blue
				type = "mode1";
				shapeString = "roundrectangle";
			}
			
			Element vClass = new Element("data", xmlns);
			vClass.setAttribute(new Attribute("key", "class"));
			vClass.setText(type);
			node.addContent(vClass);
			
			Element vis = new Element("data", xmlns);
			vis.setAttribute(new Attribute("key", "d0"));
			Element visoneShapeNode = new Element("shapeNode", visone);
			Element yShapeNode = new Element("ShapeNode", yNs);
			Element geometry = new Element("Geometry", yNs);
			geometry.setAttribute(new Attribute("height", "20.0"));
			geometry.setAttribute(new Attribute("width", "20.0"));
			geometry.setAttribute(new Attribute("x", String.valueOf(Math.random()*800)));
			geometry.setAttribute(new Attribute("y", String.valueOf(Math.random()*600)));
			yShapeNode.addContent(geometry);
			Element fill = new Element("Fill", yNs);
			fill.setAttribute(new Attribute("color", hex));
			fill.setAttribute(new Attribute("transparent", "false"));
			yShapeNode.addContent(fill);
			Element borderStyle = new Element("BorderStyle", yNs);
			borderStyle.setAttribute(new Attribute("color", "#000000"));
			borderStyle.setAttribute(new Attribute("type", "line"));
			borderStyle.setAttribute(new Attribute("width", "1.0"));
			yShapeNode.addContent(borderStyle);
			
			Element nodeLabel = new Element("NodeLabel", yNs);
			nodeLabel.setAttribute(new Attribute("alignment", "center"));
			nodeLabel.setAttribute(new Attribute("autoSizePolicy", "content"));
			nodeLabel.setAttribute(new Attribute("backgroundColor", "#FFFFFF"));
			nodeLabel.setAttribute(new Attribute("fontFamily", "Dialog"));
			nodeLabel.setAttribute(new Attribute("fontSize", "12"));
			nodeLabel.setAttribute(new Attribute("fontStyle", "plain"));
			nodeLabel.setAttribute(new Attribute("hasLineColor", "false"));
			nodeLabel.setAttribute(new Attribute("height", "19.0"));
			nodeLabel.setAttribute(new Attribute("modelName", "eight_pos"));
			nodeLabel.setAttribute(new Attribute("modelPosition", "n"));
			nodeLabel.setAttribute(new Attribute("textColor", "#000000"));
			nodeLabel.setAttribute(new Attribute("visible", "true"));
			nodeLabel.setText(labels[i]);
			yShapeNode.addContent(nodeLabel);
			
			Element shape = new Element("Shape", yNs);
			shape.setAttribute(new Attribute("type", shapeString));
			yShapeNode.addContent(shape);
			visoneShapeNode.addContent(yShapeNode);
			vis.addContent(visoneShapeNode);
			node.addContent(vis);
			
			graphElement.addContent(node);
		}
		
		Comment edgesComment = new Comment(" edges ");
		graphElement.addContent(edgesComment);
		
		for (int i = 0; i < edges.size(); i++) {
			Element edge = new Element("edge", xmlns);
			edge.setAttribute(new Attribute("source", edges.get(i).getSource()));
			edge.setAttribute(new Attribute("target", edges.get(i).getTarget()));
			Element weight = new Element("data", xmlns);
			weight.setAttribute(new Attribute("key", "weight"));
			weight.setText(String.valueOf(edges.get(i).getWeight()));
			edge.addContent(weight);
			
			Element visEdge = new Element("data", xmlns);
			visEdge.setAttribute("key", "e0");
			Element visPolyLineEdge = new Element("polyLineEdge", visone);
			Element yPolyLineEdge = new Element("PolyLineEdge", yNs);
			Element yLineStyle = new Element("LineStyle", yNs);
			String col;
			if (edges.get(i).getWeight() > 0) {
				col = "#00ff00";
			} else if (edges.get(i).getWeight() < 0) {
				col = "#ff0000";
			} else {
				col = "#000000";
			}
			yLineStyle.setAttribute("color", col);
			yLineStyle.setAttribute(new Attribute("type", "line"));
			yLineStyle.setAttribute(new Attribute("width", "2.0"));
			yPolyLineEdge.addContent(yLineStyle);
			visPolyLineEdge.addContent(yPolyLineEdge);
			visEdge.addContent(visPolyLineEdge);
			edge.addContent(visEdge);
			
			graphElement.addContent(edge);
		}
		
		graphml.addContent(graphElement);
		
		File dnaFile = new File (outfile);
		try {
			FileOutputStream outStream = new FileOutputStream(dnaFile);
			XMLOutputter outToFile = new XMLOutputter();
			Format format = Format.getPrettyFormat();
			format.setEncoding("utf-8");
			outToFile.setFormat(format);
			outToFile.output(document, outStream);
			outStream.flush();
			outStream.close();
		} catch (IOException e) {
			System.err.println("Cannot save \"" + dnaFile + "\":" + e.getMessage());
			JOptionPane.showMessageDialog(Dna.dna.gui, "Error while saving the file!\n" + e.getStackTrace());
		}
	}
}
