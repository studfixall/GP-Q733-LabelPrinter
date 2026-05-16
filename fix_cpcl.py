import re

with open('app/src/main/java/com/gp/q733/domain/print/PrintProtocol.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace CPCL Text handling - simpler pattern
old_text = '''                        // Always use standard fonts 0-6, font 55 may not be supported
                        val font = fontSize'''

new_text = '''                        // Check if text contains Chinese characters
                        val hasChinese = element.text.any { it.code > 0x4E00 && it.code < 0x9FFF }
                        // Use font 55 for Chinese, standard font for ASCII
                        val font = if (hasChinese) 55 else fontSize'''

if old_text in content:
    content = content.replace(old_text, new_text)
    print('Replaced font selection')
else:
    print('Font pattern not found')

# Add UTF-8 codepage setting
old_header = '''            // CPCL uses UTF-8 by default for text, no CODEPAGE needed
            baos.write("SETMAG 1 1'''

new_header = '''            // Enable UTF-8 for Chinese support
            baos.write("! U1 SETCODEPAGE UTF-8\r\n".toByteArray(Charsets.UTF_8))
            baos.write("SETMAG 1 1'''

if old_header in content:
    content = content.replace(old_header, new_header)
    print('Replaced header')
else:
    print('Header pattern not found')

with open('app/src/main/java/com/gp/q733/domain/print/PrintProtocol.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print('Done')
