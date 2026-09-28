path = r'C:\Users\markv\AndroidStudioProjects\DormigoVA\app\src\main\res\layout\activity_notifications.xml'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

import re

# We want to replace the whole <LinearLayout> inside the <ScrollView> that starts with <LinearLayout android:layout_width="match_parent"...
# and ends with </ScrollView>
start_str = '''    <!-- Notification List -->
    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:fillViewport="true"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintTop_toBottomOf="@id/filtersScroll">'''

end_str = '''    </ScrollView>

</androidx.constraintlayout.widget.ConstraintLayout>'''

start_idx = content.find(start_str)
end_idx = content.find(end_str)

if start_idx != -1 and end_idx != -1:
    new_content = content[:start_idx] + start_str + '''

        <LinearLayout
            android:id="@+id/notifListLayout"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingBottom="24dp">

        </LinearLayout>
''' + end_str
    with open(path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print("Updated xml layout.")
else:
    print("Could not find boundaries.")
