import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditableList } from './editable-list';

describe('EditableList', () => {
  let fixture: ComponentFixture<EditableList>;
  let component: EditableList;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditableList],
    }).compileComponents();

    fixture = TestBed.createComponent(EditableList);
    fixture.componentRef.setInput('label', 'Decisões');
    fixture.componentRef.setInput('items', ['Fechar contrato']);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('adds a trimmed item and clears the draft input', () => {
    component['draftItem'] = '  Enviar proposta  ';
    component['addItem']();

    expect(component.items()).toEqual(['Fechar contrato', 'Enviar proposta']);
    expect(component['draftItem']).toBe('');
  });

  it('ignores empty input', () => {
    component['draftItem'] = '   ';
    component['addItem']();

    expect(component.items()).toEqual(['Fechar contrato']);
  });

  it('removes an item by index', () => {
    component['removeItem'](0);
    expect(component.items()).toEqual([]);
  });
});
